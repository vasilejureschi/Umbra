package org.unchiujar.umbra.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.location.Location;

import com.xtremelabs.robolectric.RobolectricTestRunner;
@RunWith(RobolectricTestRunner.class)
public class LocationUtilsTest {

	@Test
	public void haversineDistance() {
		Location location1 = new Location("test");
		location1.setLatitude(-73.995008d);
		location1.setLongitude(40.752842d);
		
		Location location2 = new Location("test");
		location2.setLatitude(-73.994905d);
		location2.setLongitude(40.752798d);

		
		double actual = LocationUtilities.haversineDistance(location1, location2);
		double expected= location1.distanceTo(location2);
		assertEquals(expected, actual, 0.2d);
	}
	
	
	@Test
	public void haversineDistanceNegative() {
		Location location1 = new Location("test");
		location1.setLatitude(-73.995008d);
		location1.setLongitude(-40.752842d);
		
		Location location2 = new Location("test");
		location2.setLatitude(-73.994905d);
		location2.setLongitude(-40.752798d);

		
		double actual = LocationUtilities.haversineDistance(location1, location2);
		double expected= location1.distanceTo(location2);
		assertEquals(expected, actual, 0.2d);
	}

	@Test
	public void haversineDistanceZero() {
		Location location1 = new Location("test");
		location1.setLatitude(0);
		location1.setLongitude(0);
		
		Location location2 = new Location("test");
		location2.setLatitude(0);
		location2.setLongitude(0);

		
		double actual = LocationUtilities.haversineDistance(location1, location2);
		double expected= location1.distanceTo(location2);
		assertEquals(expected, actual, 0.0d);
	}
	
}
