package com.tarkrakov.tearsofink.world;

/** Shared immutable-road guard for new engineered temple approaches.
 * The temple footprint guard remains 24 blocks; only paths may use the
 * natural ground outside the actual lane/shoulder/post columns.
 */
final class TempleRoadAccess {
    static final int CUT=7;
    private TempleRoadAccess() {}
    static boolean canBuild(InkLandsLayout layout,int x,int z) {
        if (layout.depthInside(x,z)<0 || layout.roadNear(x,z,InkLandsLayout.ROAD_SHOULDER)!=null) return false;
        var near=layout.roadNear(x,z,TempleNetwork.ROAD_BUFFER-InkLandsLayout.ROAD_HALF_WIDTH);
        if (near==null) return true;
        int road=(int)near[0];
        if (layout.roadBridge(road,near[1])) return false;
        for (int n=0;n<InkLandsLayout.OUTPOSTS.length;n++)
            if (Math.abs(near[1]-layout.outpostDistance(n))<72) return false;
        int k=(int)Math.floor(near[1]/InkLandsLayout.ROAD_LANTERN_SPACING);
        for (int n=k-2;n<=k+2;n++) {
            int[] post=layout.roadLanternPost(road,n);
            if (post!=null && Math.abs(x-post[0])<=1 && Math.abs(z-post[1])<=1) return false;
        }
        return true;
    }
}
