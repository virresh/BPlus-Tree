import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Driver {
	
	static BPlusTree<KeyNodes,List<Integer>> btree;
	static int NUM_RECS = 50;
	static int colNum=0;
	static String indexFile = "index.dat";
	
	public Driver() {
		
	}
	
	static Scanner s = new Scanner(System.in);
	
	static void writeTreeToFile() throws IOException
	{
		
		FileOutputStream f = new FileOutputStream(new File(indexFile));
		ObjectOutputStream o = new ObjectOutputStream(f);

		// Write objects to file
		o.writeObject(btree);

		o.close();
		f.close();
	}
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		System.out.println("Enter the record Type to build up the index on: (1/2/3/4)");
		colNum = s.nextInt();
		// 1 is id number
		// 2 is instructor name
		// 3 is dept name
		// 4 is salary
		if(DataFileReader.getNumberOfRecordsOnDisk()==0)
		{
			DataFileReader.writeNRecs(NUM_RECS);
		}
		else {
			NUM_RECS = (int) DataFileReader.getNumberOfRecordsOnDisk();
		}
		DataFileReader.initialise();
		buildTree();
		try {
			writeTreeToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int j=0;
		do
		{
			System.out.println("What do you want to do ?\n0. Quit\n1. Find V\n2. Find All V");
			System.out.println("3. Range Query of lV to rV\n4. Add a new node\n5. Delete V");
			j = s.nextInt();
			switch(j)
			{
			case 0:
				break;
			case 1:
				singleFindPrintOne();
				break;
			case 2:
				singleFind();
				break;
			case 3:
				rangeFind();
				break;
			case 4:
				insertData();
				try {
					writeTreeToFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case 5:
				deleteRecord();
				try {
					writeTreeToFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			default:
				System.out.println("Invalid selection");
				break;
			}
		}while(j!=0);
		
		DataFileReader.saveData();
	}
	
	static void deleteRecord()
	{
		System.out.println("Enter the Value to delete from Tree:");
		String v = s.next();
		List<Integer> res = btree.search(new KeyNodes(v));
		if(res==null)
		{
			System.out.println("No such record exists !");
		}
		else
		{
			btree.delete(new KeyNodes(v));
			for(Integer I : res)
			{
				DataFileReader.removeRecord(I);
			}
			System.out.println("Deleted all records with value V");
		}
	}
	
	static void insertData()
	{
		System.out.println("Adding new data Entry:");
		DataNode dn = DataFileReader.addRecord();
		NUM_RECS++;
		addToBPlusTree(dn,NUM_RECS);
	}
	
	static void rangeFind()
	{
		System.out.println("Enter L: ");
		String l = s.next();
		System.out.println("Enter R: ");
		String r = s.next();
		
		List<List<Integer>> queryResult = btree.searchRange(new KeyNodes(l), new KeyNodes(r)); 
		
		for(List<Integer> lI: queryResult)
		{
			for(Integer I: lI)
			{
				System.out.println(new DataNode(DataFileReader.readNthRecord(I)));
			}
		}
	}
	
	static void singleFindPrintOne()
	{
		System.out.println("Enter a value: ");
		String q = s.next();
		List<Integer> offsets = btree.search(new KeyNodes(q));
		if(offsets == null)
		{
			System.out.println("No such record.");
		}
		else
		{
			System.out.println(new DataNode(DataFileReader.readNthRecord(offsets.get(0))));
		}
	}
	
	static void singleFind()
	{
		System.out.println("Enter a value: ");
		String q = s.next();
		List<Integer> offsets = btree.search(new KeyNodes(q));
		if(offsets == null)
		{
			System.out.println("No such record.");
		}
		else
		{
			for(Integer offset: offsets)
				System.out.println(new DataNode(DataFileReader.readNthRecord(offset)));
		}
	}
	
	static void addToBPlusTree(DataNode p, Integer offset)
	{
		if(!(1<=colNum && colNum<=4))
		{
			System.out.println("Invalid Column");
			return;
		}
		
		KeyNodes kn = null;
		if(colNum==1)
		{
			kn = new KeyNodes(p.getId());
//			System.out.println(kn.key);
		}
		else if(colNum==2)
		{
			kn = new KeyNodes(p.getName());
		}
		else if(colNum==3)
		{
			kn = new KeyNodes(p.getDept());
		}
		else if(colNum==4)
		{
			kn = new KeyNodes(p.getSalary());
		}
		
		List<Integer> x = btree.search(kn);
		if(x!= null)
		{
			x.add(offset);	// add to the overflow bucket
		}
		else
		{
			x = new ArrayList<Integer>();	// create a new overflow bucket
			x.add(offset);
			btree.insert(kn, x);
		}
	}
	
	static void buildTree()
	{		
		Integer offSet = 0;
		btree = new BPlusTree<KeyNodes,List<Integer>>();
		for(; offSet < NUM_RECS; offSet++)
		{
			// offset will be the actual value in the (key,value) pair
			DataNode p = new DataNode(DataFileReader.readNthRecord(offSet));
			if(!p.validityTag.equals("0000"))
				addToBPlusTree(p,offSet);
		}
	}	
}

class KeyNodes implements Comparable<KeyNodes>,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9170519126692055519L;
	String key;
	public KeyNodes(String k) {
		key = k;
	}
	@Override
	public int compareTo(KeyNodes o) {
		return key.compareTo(o.key);
	}
}

class BPlusTree<K extends Comparable<K>, T> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -264095136088478702L;

	private static final int DEFAULT_PARAMETER = 10;
	private int param;	// BPlus tree parameter, that is variable
	private Node root;

	public BPlusTree() {
		this(DEFAULT_PARAMETER);
	}

	public BPlusTree(int branchingFactor) {
		if (branchingFactor <= 2)
			throw new IllegalArgumentException("Branching factor cannot be less than 3");
		this.param = branchingFactor;
		root = new LeafNode();
	}

	public T search(K key) {
		return root.getValue(key);
	}

	public List<T> searchRange(K key1, K key2) {
		return root.getRange(key1, key2);
	}
	
	public void insert(K key, T value) {
		root.insertValue(key, value);
	}
	
	public void delete(K key) {
		root.deleteValue(key);
	}
	
	private abstract class Node implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -7164517773102486715L;
		List<K> keys;
		
		public int getSortedLocation(List<K> keys, K key)
		{
			int pos = 0;
			for(int i=0; i<keys.size(); i++,pos++)
			{
				if(keys.get(i).compareTo(key) == 0)
				{
					return i;
				}
				else if(keys.get(i).compareTo(key) == 1)
				{
					break;
				}
			}
			return -pos-1;
		}
		int keyNum() {
			return keys.size();
		}

		abstract T getValue(K key);

		abstract void deleteValue(K key);

		abstract void insertValue(K key, T value);

		abstract K getFirstLeafKey();

		abstract List<T> getRange(K key1, K key2);

		abstract void merge(Node sibling);

		abstract Node split();

		abstract boolean isOverflow();

		abstract boolean isUnderflow();

		public String toString() {
			return keys.toString();
		}
	}

	private class InternalNode extends Node {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7134859561077657895L;
		List<Node> children;

		InternalNode() {
			this.keys = new ArrayList<K>();
			this.children = new ArrayList<Node>();
		}

		@Override
		T getValue(K key) {
			return getChild(key).getValue(key);
		}

		@Override
		void deleteValue(K key) {
			Node child = getChild(key);
			// first handle deletion at the leaf levels
			child.deleteValue(key);
			
			// check if underflow after deletion
			if (child.isUnderflow()) {
				Node childLeftSibling = getChildLeftSibling(key);
				Node childRightSibling = getChildRightSibling(key);
				Node left = childLeftSibling != null ? childLeftSibling : child;
				Node right = childLeftSibling != null ? child
						: childRightSibling;
				left.merge(right);
				deleteChild(right.getFirstLeafKey());
				if (left.isOverflow()) {
					Node sibling = left.split();
					insertChild(sibling.getFirstLeafKey(), sibling);
				}
				if (root.keyNum() == 0)
					root = left;
			}
		}

		@Override
		void insertValue(K key, T value) {
			Node child = getChild(key);
			child.insertValue(key, value);
			if (child.isOverflow()) {
				Node sibling = child.split();
				insertChild(sibling.getFirstLeafKey(), sibling);
			}
			if (root.isOverflow()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.keys.add(sibling.getFirstLeafKey());
				newRoot.children.add(this);
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		@Override
		K getFirstLeafKey() {
			return children.get(0).getFirstLeafKey();
		}

		@Override
		List<T> getRange(K key1, K key2) {
			return getChild(key1).getRange(key1, key2);
		}

		@Override
		void merge(Node sibling) {
			InternalNode node = (InternalNode) sibling;
			keys.add(node.getFirstLeafKey());
			keys.addAll(node.keys);
			children.addAll(node.children);

		}

		@Override
		Node split() {
			int from = keyNum() / 2 + 1;
			int to = keyNum();
			// split from 'from' number of children to 'to' number
			InternalNode sibling = new InternalNode();
			sibling.keys.addAll(keys.subList(from, to));
			sibling.children.addAll(children.subList(from, to + 1));

			keys.subList(from - 1, to).clear();
			children.subList(from, to + 1).clear();

			return sibling;
		}

		@Override
		boolean isOverflow() {
			return children.size() > param;
		}

		@Override
		boolean isUnderflow() {
			return children.size() < (param + 1) / 2;
		}

		Node getChild(K key) {
//			int loc = Collections.binarySearch(keys,key);
			int loc = Collections.binarySearch(keys,key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			return children.get(childIndex);
		}

		void deleteChild(K key) {
			int loc = Collections.binarySearch(keys,key);
			if (loc >= 0) {
				keys.remove(loc);
				children.remove(loc + 1);
			}
		}

		void insertChild(K key, Node child) {
			int loc = Collections.binarySearch(keys,key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (loc >= 0) {
				children.set(childIndex, child);
			} else {
				keys.add(childIndex, key);
				children.add(childIndex + 1, child);
			}
		}

		Node getChildLeftSibling(K key) {
			int loc = Collections.binarySearch(keys,key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (childIndex > 0)
				return children.get(childIndex - 1);

			return null;
		}

		Node getChildRightSibling(K key) {
			int loc = Collections.binarySearch(keys,key);
			int childIndex = loc >= 0 ? loc + 1 : -loc - 1;
			if (childIndex < keyNum())
				return children.get(childIndex + 1);

			return null;
		}
	}

	private class LeafNode extends Node {
		/**
		 * 
		 */
		private static final long serialVersionUID = -512521536974646703L;
		List<T> values;
		LeafNode next;

		LeafNode() {
			keys = new ArrayList<K>();
			values = new ArrayList<T>();
		}

		@Override
		T getValue(K key) {
			int loc = Collections.binarySearch(keys,key);
			return loc >= 0 ? values.get(loc) : null;
		}

		@Override
		void deleteValue(K key) {
			int loc = Collections.binarySearch(keys,key);
			if (loc >= 0) {
				keys.remove(loc);
				values.remove(loc);
			}
		}

		@Override
		void insertValue(K key, T value) {
			int loc = Collections.binarySearch(keys,key);
			int valueIndex = loc >= 0 ? loc : -loc - 1;
			if (loc >= 0) {
				values.set(valueIndex, value);
			} else {
				keys.add(valueIndex, key);
				values.add(valueIndex, value);
			}
			if (root.isOverflow()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.keys.add(sibling.getFirstLeafKey());
				newRoot.children.add(this);
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		@Override
		K getFirstLeafKey() {
			return keys.get(0);
		}

		@Override
		List<T> getRange(K key1, K key2) {
			List<T> result = new ArrayList<T>();
			LeafNode node = this;
			while (node != null) {
				for (int i = 0; i < keyNum(); i++) {
					int cmp1 = node.keys.get(i).compareTo(key1);
					int cmp2 = node.keys.get(i).compareTo(key2);
					if (cmp1 >= 0 && cmp2 <= 0)
						result.add(node.values.get(i));
					else if (cmp2 > 0)
						return result;
				}
				node = node.next;
			}
			return result;
		}

		@Override
		void merge(Node sibling) {
			LeafNode node = (LeafNode) sibling;
			keys.addAll(node.keys);
			values.addAll(node.values);
			next = node.next;
		}

		@Override
		Node split() {
			LeafNode sibling = new LeafNode();
			int from = (keyNum() + 1) / 2;
			int to = keyNum();
			// split from 'from' number of children to 'to' number
			sibling.keys.addAll(keys.subList(from, to));
			sibling.values.addAll(values.subList(from, to));

			keys.subList(from, to).clear();
			values.subList(from, to).clear();

			sibling.next = next;
			next = sibling;
			return sibling;
		}

		@Override
		boolean isOverflow() {
			return values.size() > param - 1;
		}

		@Override
		boolean isUnderflow() {
			return values.size() < param / 2;
		}
	}
}

class DataFileReader {
	
	static String fileName = "data.txt";
	static List<DataNode> data;
	public DataFileReader() {
		
	}
	
	public static void initialise()
	{
		long p = getNumberOfRecordsOnDisk();
		data = new ArrayList<DataNode>();
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			for(long u=0; u<p; u++)
			{
				line = br.readLine();
				data.add(new DataNode(line));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static Boolean fileExists()
	{
		File tmpFile = new File(fileName);
		boolean exists = tmpFile.exists();
		return exists;
	}
	
	public static long getNumberOfRecords()
	{
		return data.size();
	}
	
	public static long getNumberOfRecordsOnDisk()
	{
		File tmpFile = new File(fileName);
		boolean exists = tmpFile.exists();
		if(exists == false)
			return 0;
		else
			return tmpFile.length()/49;
	}
	
	public static String readNthRecord(int n)
	{
		// n is the offset, 0 means first record
		// 1 means second record ...
		return data.get(n).serialString();
	}
	
	public static String readNthRecordFromDisk(int n)
	{
		// n is the offset, 0 means first record
		// 1 means second record
		// 2 means third record
		// 3 means fourth record ...
		String line = null;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
		    for (int i = 0; i < n; i++)
		        br.readLine();
		    line = br.readLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return line;
	}
	
	public static void writeNRecs(int n) throws FileNotFoundException, UnsupportedEncodingException
	{
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		for(int i=0; i<n; i++)
		{
			DataNode p = DataNode.getRandomNode();
			writer.println(p.serialString());
		}
		writer.close();
	}
	
	public static void saveData() throws FileNotFoundException, UnsupportedEncodingException
	{
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		for(int i=0; i<data.size(); i++)
		{
			DataNode p = data.get(i);
			writer.println(p.serialString());
		}
		writer.close();
	}

	public static DataNode addRecord()
	{
		DataNode p = DataNode.getRandomNode();
		System.out.println(p);
		data.add(p);
		return p;
	}
	
	public static void removeRecord(int offset)
	{
		data.get(offset).validityTag = "0000";
	}
	
	public static void main(String[] args) {
		
		try {
			writeNRecs(5);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println(new DataNode(readNthRecord(2)));
	}

}

class DataNode {
	String validityTag;	//  4 bytes
	String id;			//  4 bytes
	String name;		// 20 bytes
	String dept;		// 10 bytes
	int salary;			// 10 bytes
	

	private static final String ALPHA_STRING = "abcdefghijklmnopqrstuvwxyz";
	public static String randomAlphaNumeric(int count) {
		StringBuilder builder = new StringBuilder();
		while (count-- != 0) {
			int character = (int)(Math.random()*ALPHA_STRING.length());
			builder.append(ALPHA_STRING.charAt(character));
		}
		return builder.toString();
	}
	
	public DataNode(String row) {
		if(row.length() != 48)
		{
			throw new NullPointerException("Invalid Record Type.");
		}
		validityTag = row.substring(0,4);
		id = row.substring(4,8);
		name = row.substring(8,28);
		dept = row.substring(28,38);
		salary = Integer.parseInt(row.substring(38,48));
	}
	
	public static DataNode getRandomNode()
	{
		Random h = new Random();
		String vTag = ""+(h.nextInt(9999-1000)+1000);
		String nid = String.format("%04d",h.nextInt(9998)+1);
		String nname = randomAlphaNumeric(20);
		String ndept = randomAlphaNumeric(10);
		String nSal = String.format("%010d",h.nextInt(100000)+1000);
		return new DataNode(vTag+nid+nname+ndept+nSal);
	}
	
	public String serialString()
	{
		return String.format("%1$4s",validityTag)+String.format("%1$4s",id)+String.format("%1$20s",name)+String.format("%1$10s",dept)+String.format("%010d",salary);
	}
	
	@Override
	public String toString()
	{
		return getValidityTag()+" "+getId()+" "+getName()+" "+getDept()+" "+getSalary();
	}

	/**
	 * @return the validityTag
	 */
	public String getValidityTag() {
		return String.format("%1$4s",validityTag);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return String.format("%1$4s",id);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return String.format("%1$20s",name);
	}

	/**
	 * @return the dept
	 */
	public String getDept() {
		return String.format("%1$10s",dept);
	}

	/**
	 * @return the salary
	 */
	public String getSalary() {
		return String.format("%010d",salary);
	}
}