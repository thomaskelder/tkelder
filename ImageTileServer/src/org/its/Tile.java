package org.its;

public class Tile {
	private byte[] data;
	private int x, y, z;
	
	public Tile(int x, int y, int z, byte[] data) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}
}
