import java.util.HashMap;

public class disp{
	
	public int num_of_rows;
	public HashMap<Integer,String[]> content;
	public String[] columnName;
	public int[] format;
	
	public disp()
	{
		num_of_rows=0;
		content=new HashMap<Integer,String[]>();
	}
	


	public void display(String[] columns)
	{
		if(num_of_rows==0)
			System.out.println("It is an empty set.");
		else
		{
			update_format();
			
			if(columns[0].equals("*"))
			{
				for(int i:format)
					System.out.print(DavisBase.line("-",i+3));
				
				System.out.println();
				
				for(int i=0;i<columnName.length;i++)
					System.out.print(format_string(format[i], columnName[i])+"|");
				
				System.out.println();
				
				for(int i:format)
					System.out.print(DavisBase.line("-",i+3));
				
				System.out.println();
				
				for(String[] i:content.values())
				{
					for(int j=0;j<i.length;j++)
						System.out.print(format_string(format[j],i[j])+"|");
					System.out.println();
				}
				
			}
			else
			{
				int [] require_cols=new int[columns.length];
				for(int i=0;i<columns.length;i++)
				{
					for(int j=0;j<columnName.length;j++)
					{
						if(columns[i].equals(columnName[j]))
							require_cols[i]=j;
					}
				}
				
				for(int i=0;i<require_cols.length;i++)
					System.out.print(DavisBase.line("-",format[require_cols[i]]+3));
				
				System.out.println();
				
				for(int i=0;i<require_cols.length;i++)
					System.out.println(format_string(format[require_cols[i]], columnName[require_cols[i]])+"|");
				
				for(int i=0;i<require_cols.length;i++)
					System.out.println(DavisBase.line("-",format[require_cols[i]]+3));
				
				for(String[] i:content.values())
				{
					for(int j=0;j<require_cols.length;j++)
						System.out.println(format_string(format[require_cols[j]],i[require_cols[j]])+"|");
				}
				System.out.println();
			}
		}
	}



	public void add(int rowid,String[] value)
	{
		content.put(rowid,value);
		num_of_rows+=1;
	}
	
	public String format_string(int length,String s)
	{
		return String.format("%-"+(length+3)+"s",s);
	}
	
	public void update_format()
	{
		for(int i=0;i<format.length;i++)
			format[i]=columnName[i].length();
		for(String[] val: content.values())
		{
			for(int j=0;j<val.length;j++)
			{
				if(format[j]<val[j].length())
					format[j]=val[j].length();
			}
		}
	}
	



}