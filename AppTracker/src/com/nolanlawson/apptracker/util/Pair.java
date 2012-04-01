package com.nolanlawson.apptracker.util;

public class Pair<E,T> {

	private E first;
	private T second;
	
	public Pair(E first, T second) {
		this.first = first;
		this.second = second;
	}

	public E getFirst() {
		return first;
	}

	public void setFirst(E first) {
		this.first = first;
	}

	public T getSecond() {
		return second;
	}

	public void setSecond(T second) {
		this.second = second;
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + "]";
	}
	
	
	
}
