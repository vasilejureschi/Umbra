package org.unchiujar.umbra.overlays;

import android.content.res.AssetManager;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

public class ExploredTileProvider implements TileProvider {

    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final int BUFFER_SIZE = 16 * 1024;


    public ExploredTileProvider(AssetManager assets) {
        mAssets = assets;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        byte[] image = readTileImage(x, y, zoom);
        return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);
    }

}
