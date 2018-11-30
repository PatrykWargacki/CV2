package ar.wargus.cv2.utils;

public class Triplet<X, Y, Z> {
	
	public final X first;
	public final Y second;
	public final Z third;
	
	public Triplet(X first, Y second, Z third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
}
