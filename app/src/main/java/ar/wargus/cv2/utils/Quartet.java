package ar.wargus.cv2.utils;

public class Quartet<V, X, Y, Z> {
	
	public final V first;
	public X second;
	public final Y third;
	public final Z fourth;
	
	public Quartet(V first, X second, Y third, Z fourth) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}
}
