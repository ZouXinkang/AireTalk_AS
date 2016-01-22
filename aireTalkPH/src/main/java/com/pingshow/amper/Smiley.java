package com.pingshow.amper;

public class Smiley {
	
	final public static int MAXSIZE=SmileyActivity.smiles.length;
	final public static int MAX_REPEAT=48;
	final public static Object[][] smiles=SmileyActivity.smiles;
	
	int [] matches=new int[MAXSIZE];
	int [][] start=new int[MAXSIZE][MAX_REPEAT];
			
	public int hasSmileys(String ss)
	{
		String s;
		int j;
		for (int i=0;i<MAXSIZE;i++)
		{
			j=0;
			s=ss;
			while(s!=null && s.contains((String)smiles[i][0]) && matches[i]<MAX_REPEAT)
			{
				j+=s.indexOf((String)smiles[i][0]);
				start[i][matches[i]++]=j;
				j+=((String)smiles[i][0]).length();
				if (j>=ss.length()-1)
					break;
				s=ss.substring(j);
			}
		}
		int hasSM=0;
		for (int i=0;i<MAXSIZE;i++)
			hasSM+=matches[i];
		return hasSM;
	}
	public int getCount(int index)
	{
		return matches[index];
	}
	public int getStart(int index, int ind)
	{
		return start[index][ind];
	}
	public int getEnd(int index, int ind)
	{
		return start[index][ind]+((String)smiles[index][0]).length();
	}
}
