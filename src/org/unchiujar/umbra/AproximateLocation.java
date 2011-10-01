package org.unchiujar.umbra;

import android.location.Location;

public class AproximateLocation extends Location {

    private double x;
    private double y;
    private double z;
    
    public AproximateLocation(String provider) {
        super(provider);
    }

    public AproximateLocation(Location l) {
        super(l);
    }

    private void updateNVector(){
        x = Math.cos(getLatitude())* Math.cos(getLongitude());
        y = Math.cos(getLatitude())* Math.sin(getLongitude());
        z = Math.sin(getLatitude());
    }
    @Override
    public void setLatitude(double latitude) {
        // TODO Auto-generated method stub
        super.setLatitude(latitude);
        updateNVector();
    }

    @Override
    public void setLongitude(double longitude) {
        // TODO Auto-generated method stub
        super.setLongitude(longitude);
        updateNVector();
    }

    @Override
    public boolean equals(Object location) {
        // sanity checks
        if (!(location instanceof AproximateLocation)) {
            return false;
        }
        // check if it is outside the preset radius
        if (this.distanceTo((AproximateLocation) location) > LocationOrder.METERS_RADIUS) {
            return false;
        }
        return true;
    }

    // TODO - check theory behind implementation, see Effective Java
    @Override
    public int hashCode() {
        int randomPrime = 47;
        int result = 42;
        long hashLong = Double.doubleToLongBits(this.getLongitude());
        long hashLat = Double.doubleToLongBits(this.getLatitude());
        result = (int) (randomPrime * result + hashLong);
        result = (int) (randomPrime * result + hashLat);
        return result;
    }
}
