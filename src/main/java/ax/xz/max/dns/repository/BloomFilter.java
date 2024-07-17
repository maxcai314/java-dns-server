package ax.xz.max.dns.repository;

import java.util.BitSet;

/**
 * This class is not thread-safe.
 */
class BloomFilter<T> {
	private final int numBits;
	private final int numHashes;
	private final BitSet bits;

	public BloomFilter(int numBits, int numHashes) {
		this.numBits = numBits;
		this.numHashes = numHashes;

		this.bits = new BitSet(numBits);
	}

	private static final int C1 = 0xcc9e2d51;
	private static final int C2 = 0x1b873593;

	private static int smear(int hashCode) {
		return (C2 * Integer.rotateLeft((hashCode * C1), 15));
	}

	private int indexFor(int hashCode, int hashNumber) {
		int hash = smear(hashCode * 31 + hashNumber);
		return Math.floorMod(hash, numBits);
	}

	public void add(T element) {
		int hashCode = element.hashCode();
		for (int i=0; i<numHashes; i++)
			bits.set(indexFor(hashCode, i));
	}

	public boolean neverContains(T element) {
		int hashCode = element.hashCode();
		for (int i=0; i<numHashes; i++)
			if (!bits.get(indexFor(hashCode, i)))
				return true;
		return false;
	}

	public void clear() {
		bits.clear();
	}

	public double theoreticalFalsePositiveRate(int numItems) {
		return Math.pow(-Math.expm1((double) -numHashes * numItems / numBits), numHashes);
	}

	public double falsePositiveRate() {
		return Math.pow((double) bits.cardinality() / numBits, numHashes);
	}

	public static <T> BloomFilter<T> optimalFilterFor(int numElements, double falsePositiveRate) {
		numElements = Math.max(numElements, 2);
		double numHashes =  -Math.log(falsePositiveRate) / Math.log(2);
		double bitsPerElement = numHashes / Math.log(2);

		return new BloomFilter<>((int) (bitsPerElement * numElements + 0.5), (int) (numHashes + 0.5));
	}

}
