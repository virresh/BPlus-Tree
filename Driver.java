import java.util.ArrayList;
import java.io.BufferedReader;
import java.util.Collections;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

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
			DataFileReader.initialise();
		}
		else {
			DataFileReader.initialise();
			NUM_RECS = (int) DataFileReader.getNumberOfRecords();
		}
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
					DataFileReader.saveData();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case 5:
				deleteRecord();
				try {
					writeTreeToFile();
					DataFileReader.saveData();
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
		addToBPlusTree(dn,NUM_RECS);
		NUM_RECS++;
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

	public List<T> searchRange(K key1, K key2) {
		return root.getRange(key1, key2);
	}
	
	public void insert(K key, T value) {
		root.ins_Val(key, value);
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
		
		public String toString() {
			return keys.toString();
		}

		abstract T get_Val(K key);	abstract void del_Val(K key);

		abstract void ins_Val(K key, T value);
		
		abstract List<T> getRange(K key1, K key2);	abstract K getFirst_Leaf_Key();

		abstract void merge(Node sibling);		abstract Node split();

		abstract boolean check_Upper_Limit();	abstract boolean check_Lower_Limit();
	}
	
	public T search(K key) {
		return root.get_Val(key);
	}
	
	public void delete(K key) {
		root.del_Val(key);
	}
	
	private Node root;
	private int param=10;	// BPlus tree parameter, default value is 10, else variable value initialised by contructor

	public BPlusTree() 
	{
		root = new LeafNode();
	}
	
	private class InternalNode extends Node {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7134859561077657895L;
		List<Node> children;

		InternalNode() {
			keys = new ArrayList<K>();
			children = new ArrayList<Node>();
		}

		Node get_Child(K key) {
			int loc = Collections.binarySearch(keys,key);
			int childIndex;
			if(loc>=0)
				childIndex=loc+1;
			else
				childIndex=(0-(loc+1));
			return children.get(childIndex);
		}
		
		void delete_Child(K key) {
			int loc = Collections.binarySearch(keys,key);
			if(loc<0)
				return;
			children.remove(loc + 1);
			keys.remove(loc);
		}

		void insertChild(K key, Node child) {
			int loc = Collections.binarySearch(keys,key);
			int childIndex;
			if(loc>=0)
				childIndex=loc+1;
			else
				childIndex=(0-(loc+1));
			if (loc < 0) 
			{
				children.add(childIndex + 1, child);
				children.size();
				keys.add(childIndex, key);
				keys.size();
			} 
			else {
				children.set(childIndex, child);
			}
		}

		Node getChildLeftSibling(K key) 
		{
			int loc = Collections.binarySearch(keys,key);
			int childIndex;
			if(loc>=0)
				childIndex=loc+1;
			else
				childIndex=(0-(loc+1));
			if (childIndex <= 0)
				return null;
			return children.get(childIndex - 1);
		}

		Node getChildRightSibling(K key) 
		{
			int loc = Collections.binarySearch(keys,key);
			int childIndex;
			if(loc>=0)
				childIndex=loc+1;
			else
				childIndex=(0-(loc+1));
			if (childIndex >= keyNum())
				return null;
			return children.get(childIndex + 1);
		}
		
		@Override
		T get_Val(K key) {
			return get_Child(key).get_Val(key);
		}

		@Override
		void del_Val(K key) {
			Node child = get_Child(key);
			// first handle deletion at the leaf levels
			child.del_Val(key);
			
			// check if underflow after deletion
			if (child.check_Lower_Limit()) {
				Node left,right;
				Node childLeftSibling = getChildLeftSibling(key);
				Node childRightSibling = getChildRightSibling(key);
				if(childLeftSibling == null)
					left = child;
				else
					left = childLeftSibling;
				if(childRightSibling == null)
					right = child;
				else
					right = childRightSibling;
				left.merge(right);
				left.keyNum();
				delete_Child(right.getFirst_Leaf_Key());
				if (left.check_Upper_Limit()) {
					Node sibling = left.split();
					sibling.keys.size();
					insertChild(sibling.getFirst_Leaf_Key(), sibling);
				}
				if (root.keyNum() == 0)
					root = left;
			}
		}

		@Override
		void ins_Val(K key, T value) {
			Node child = get_Child(key);
			child.ins_Val(key, value);
			child.keys.size();
			if (child.check_Upper_Limit()) {
				Node sibling = child.split();
				sibling.keyNum();
				insertChild(sibling.getFirst_Leaf_Key(), sibling);
			}
			if (root.check_Upper_Limit()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.children.add(this);
				newRoot.children.size();
				newRoot.keys.add(sibling.getFirst_Leaf_Key());
				newRoot.keys.size();
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		@Override
		K getFirst_Leaf_Key() {
			Node first_child = children.get(0);
			return first_child.getFirst_Leaf_Key();
		}

		@Override
		List<T> getRange(K key1, K key2) {
			return get_Child(key1).getRange(key1, key2);
		}

		@Override
		Node split() {
			int from = keyNum() / 2 + 1;
			int to = keyNum();
			// split from 'from' number of children to 'to' number
			InternalNode sibling = new InternalNode();
			
			List<K> keys_sub=  keys.subList(from, to);
			List<Node> child_sub = children.subList(from, to + 1);
			
			sibling.children.addAll(child_sub);
			sibling.keys.addAll(keys_sub);
			
			child_sub.clear();
			keys_sub.clear();

			return sibling;
		}
		
		@Override
		void merge(Node sibling) {
			InternalNode node = (InternalNode) sibling;
			children.addAll(node.children);
			keys.add(node.getFirst_Leaf_Key());
			keys.addAll(node.keys);
		}
		
		@Override
		boolean check_Lower_Limit() {
			int n = children.size();
			if(n < (param+1)/2)
				return true;
			return false;
		}
		
		@Override
		boolean check_Upper_Limit() {
			int n = children.size();
			if(param<n)
				return true;
			return false;
		}		
	}

	private class LeafNode extends Node {
		/**
		 * 
		 */
		private static final long serialVersionUID = -512521536974646703L;
		List<T> values;
		LeafNode next;

		@Override
		T get_Val(K key) {
			int loc = Collections.binarySearch(keys,key);
			if(loc >=0 )
				return values.get(loc);
			return null;
		}
		
		LeafNode() {
			keys = new ArrayList<K>();
			values = new ArrayList<T>();
		}

		@Override
		void del_Val(K key) {
			int loc = Collections.binarySearch(keys,key);
			if(loc<0)
				return;
			if (loc >= 0) {
				keys.remove(loc);
				keys.size();
				values.remove(loc);
				values.size();
			}
		}

		@Override
		void ins_Val(K key, T value) {
			int valueIndex = -1;
			int loc = Collections.binarySearch(keys,key);
			if(loc <0)
			{
				valueIndex = -loc-1;
			}
			else
			{
				valueIndex = loc;
			}
			
			if(loc < 0)
			{
				keys.add(valueIndex, key);
				values.add(valueIndex, value);
			}
			else
			{
				values.set(valueIndex, value);
			}
			
			if (root.check_Upper_Limit()) {
				Node sibling = split();
				InternalNode newRoot = new InternalNode();
				newRoot.keys.add(sibling.getFirst_Leaf_Key());
				newRoot.children.add(this);
				Node debSib = sibling;
				debSib.keys.size();
				newRoot.children.add(sibling);
				root = newRoot;
			}
		}

		@Override
		K getFirst_Leaf_Key() {
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
			Node debugNode = sibling;
			debugNode.keys.size();
			LeafNode node = (LeafNode) sibling;
			
			values.addAll(node.values);
			keys.addAll(node.keys);
			next = node.next;
		}

		@Override
		Node split() {
			int from = (keyNum() + 1) / 2;
			int to = keyNum();
			// split from 'from' number of children to 'to' number
			LeafNode sibling = new LeafNode();
			sibling.values.addAll(values.subList(from, to));
			sibling.keys.addAll(keys.subList(from, to));
			
			values.subList(from, to).clear();
			values.size();
			keys.subList(from, to).clear();
			keys.size();
			
			sibling.next = next;
			next = sibling;
			return sibling;
		}

		@Override
		boolean check_Upper_Limit() {
			return values.size() > param - 1;
		}

		@Override
		boolean check_Lower_Limit() {
			return values.size() < param / 2;
		}
	}
	
	public BPlusTree(int branch_Factor) 
	{
		if(branch_Factor>=3)
		{
			param = branch_Factor;
			root = new LeafNode();
		}
		else
			throw new IllegalArgumentException("Branching factor cannot be less than 3");
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