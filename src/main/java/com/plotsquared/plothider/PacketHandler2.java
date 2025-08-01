/*
 * PlotHider, an addon to hide plots for the PlotSquared plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.plothider;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.location.World;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.BitSet;

public class PacketHandler2 implements PacketListener {

    private static final DataPalette EMPTY_BLOCK_PALETTE = new DataPalette(new SingletonPalette(0), null, PaletteType.CHUNK);
    private static final DataPalette EMPTY_BIOME_PALETTE = new DataPalette(new SingletonPalette(0), null, PaletteType.BIOME);
    private static final LightData EMPTY_LIGHT_DATA = new LightData(
            true,
            new BitSet(0),
            new BitSet(0),
            new BitSet(0),
            new BitSet(0),
            0,
            0,
            new byte[0][0],
            new byte[0][0]
    );

    private final PlotHiderPlugin plugin;

    public PacketHandler2(PlotHiderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        var packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChange(event);
        } else if (packetType == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            handleMultiBlockChange(event);
        } else if (packetType == PacketType.Play.Server.CHUNK_DATA) {
            handleMapChunk(event);
        } else if (packetType == PacketType.Play.Server.SPAWN_ENTITY) {
            handleSpawnEntity(event);
        }
    }

    private void handleBlockChange(PacketSendEvent event) {
        var player = (Player) event.getPlayer();
        var plotPlayer = BukkitUtil.adapt(player);

        if (!canHide(plotPlayer)) {
            return;
        }

        var wrapper = new WrapperPlayServerBlockChange(event);
        var position = wrapper.getBlockPosition();

        var world = plotPlayer.getLocation().getWorld();
        var loc = Location.at(world, position.getX(), position.getY(), position.getZ());
        var plot = loc.getOwnedPlot();

        if (shouldHide(plot, plotPlayer)) {
            event.setCancelled(true);
        }
    }

    private void handleMultiBlockChange(PacketSendEvent event) {
        var player = (Player) event.getPlayer();
        var plotPlayer = BukkitUtil.adapt(player);

        if (!canHide(plotPlayer)) {
            return;
        }

        var wrapper = new WrapperPlayServerMultiBlockChange(event);
        var chunkPos = wrapper.getChunkPosition();

        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        var world = plotPlayer.getLocation().getWorld();
        var chunkLoc = Location.at(world, chunkX << 4, 0, chunkZ << 4);
        var plot = chunkLoc.getOwnedPlot();

        if (shouldHide(plot, plotPlayer)) {
            event.setCancelled(true);
        }
    }

    private void handleMapChunk(PacketSendEvent event) {
        var player = (Player) event.getPlayer();
        var plotPlayer = BukkitUtil.adapt(player);

        if (!canHide(plotPlayer)) {
            return;
        }

        var wrapper = new WrapperPlayServerChunkData(event);

        // 获取区块坐标 - 直接使用getX()和getZ()，这些已经是区块坐标
        int chunkX = wrapper.getColumn().getX();
        int chunkZ = wrapper.getColumn().getZ();

        var world = plotPlayer.getLocation().getWorld();
        var chunkLoc = Location.at(world, chunkX << 4, 0, chunkZ << 4);
        var plot = chunkLoc.getOwnedPlot();

        if (shouldHide(plot, plotPlayer)) {
            // 简化逻辑: 直接创建一个空的区块数据
            // 假设每个 Plot 都是完整的 Chunk，我们可以直接隐藏整个区块
            populateWithAir(wrapper);
            event.markForReEncode(true);
        }
    }

    private void handleSpawnEntity(PacketSendEvent event) {
        var player = (Player) event.getPlayer();
        var plotPlayer = BukkitUtil.adapt(player);

        if (!canHide(plotPlayer)) {
            return;
        }

        var wrapper = new WrapperPlayServerSpawnEntity(event);
        var position = wrapper.getPosition().toVector3i();

        var world = plotPlayer.getLocation().getWorld();
        var loc = Location.at(world, position.getX(), position.getY(), position.getZ());
        var plot = loc.getOwnedPlot();

        if (shouldHide(plot, plotPlayer)) {
            event.setCancelled(true);
        }
    }

    private void populateWithAir(WrapperPlayServerChunkData wrapper) {
        var oldCol = wrapper.getColumn();
        int chunkX = oldCol.getX();
        int chunkZ = oldCol.getZ();
        wrapper.setColumn(createEmptyColumn(chunkX, chunkZ, oldCol.getChunks().length));
        wrapper.setLightData(createEmptyLightData(chunkX, chunkZ, oldCol.getChunks().length));
        wrapper.setIgnoreOldData(false);
    }

    private Column createEmptyColumn(int chunkX, int chunkZ, int sectionsCount) {
        var chunkSectionArray = new BaseChunk[sectionsCount];
        var chunkSection = new Chunk_v1_18(0, EMPTY_BLOCK_PALETTE, EMPTY_BIOME_PALETTE);
        Arrays.fill(chunkSectionArray, chunkSection);

        return new Column(
                chunkX,
                chunkZ,
                true,
                chunkSectionArray,
                null
        );
    }

    private LightData createEmptyLightData(int chunkX, int chunkZ, int sectionsCount) {
        // 直接返回静态缓存的对象，因为光照数据不会被修改
        return EMPTY_LIGHT_DATA;
    }

    private boolean canHide(PlotPlayer<?> plotPlayer) {
        return !plotPlayer.hasPermission("plots.plothider.bypass") && hasPlotArea(plotPlayer.getLocation().getWorld());
    }

    private boolean hasPlotArea(World<?> world) {
        return PlotSquared.get().getPlotAreaManager().hasPlotArea(world.getName());
    }

    private boolean shouldHide(Plot plot, PlotPlayer<?> plotPlayer) {
        // 简化逻辑: 只要是没有权限访问的 Plot 就隐藏
        return plot == null ||
                plot.isDenied(plotPlayer.getUUID()) ||
                (!plot.isAdded(plotPlayer.getUUID()) && plot.getFlag(HideFlag.class));
    }
}
