import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;


public class Base{


	public static int page_size = 512;
	public static String date_pattern = "yyyy-MM-dd_HH:mm:ss";

public static void main(String[] args){}
	public static int num_pages(RandomAccessFile file){
		int num_of_pages = 0;
		try{
			num_of_pages = (int)(file.length()/(new Long(page_size)));
		}catch(Exception e){
			System.out.println(e);
		}

		return num_of_pages;
	}

		public static boolean isTableExists(String table){
		table = table+".tbl";
		
		try {
			File database_Dir = new File("data");
			String[] oldFiles;
			oldFiles = database_Dir.list();
			for (int i=0; i<oldFiles.length; i++) {
				if(oldFiles[i].equals(table))
					return true;
			}
		}
		catch (SecurityException e) {
			System.out.println("Unable to create data container directory");
			System.out.println(e);
		}

		return false;
	}

	public static byte get_type_of_data(String value, String data_type)
	{
		if(value.equals("null")){
			switch(data_type){
				case "TINYINT":     return 0x00;
				case "SMALLINT":    return 0x01;
				case "INT":			return 0x02;
				case "BIGINT":      return 0x03;
				case "REAL":        return 0x02;
				case "DOUBLE":      return 0x03;
				case "DATETIME":    return 0x03;
				case "DATE":        return 0x03;
				case "TEXT":        return 0x03;
				default:			return 0x00;
			}							
		}else{
			switch(data_type){
				case "TINYINT":     return 0x04;
				case "SMALLINT":    return 0x05;
				case "INT":			return 0x06;
				case "BIGINT":      return 0x07;
				case "REAL":        return 0x08;
				case "DOUBLE":      return 0x09;
				case "DATETIME":    return 0x0A;
				case "DATE":        return 0x0B;
				case "TEXT":        return (byte)(value.length()+0x0C);
				default:			return 0x00;
			}
		}
	}
	

    public static short field_length(byte type_of_data)
    {
		switch(type_of_data){
			case 0x00: return 1;
			case 0x01: return 2;
			case 0x02: return 4;
			case 0x03: return 8;
			case 0x04: return 1;
			case 0x05: return 2;
			case 0x06: return 4;
			case 0x07: return 8;
			case 0x08: return 4;
			case 0x09: return 8;
			case 0x0A: return 8;
			case 0x0B: return 8;
			default:   return (short)(type_of_data - 0x0C);
		}
	}

	public static boolean expressionVal(String[] values, int rowid, String[] exp, String[] column_name)
	{

		boolean check = false;
			
		if(exp.length == 0){
			check = true;
		}
		else{
			int column_position = 1;
			for(int i = 0; i < column_name.length; i++){
				if(column_name[i].equals(exp[0])){
					column_position = i + 1;
					break;
				}
			}
			
			if(column_position == 1){
				int value = Integer.parseInt(exp[2]);
				String operator = exp[1];
				switch(operator){
					case "=": if(rowid == value) 
								check = true;
							  else
							  	check = false;
							  break;
					case ">": if(rowid > value) 
								check = true;
							  else
								check = false;
						  	break;
					case ">=": if(rowid >= value) 
						        check = true;
					          else
					        	  check = false;	
				          	break;
					case "<": if(rowid < value) 
								check = true;
						  	else
						  		check = false;
						  	break;
					case "<=": if(rowid <= value) 
								check = true;
  						  	else
  						  		check = false;	
						  	break;
					case "!=": if(rowid != value)  
								check = true;
						  	else
						  		check = false;	
						  	break;						  							  							  							
					}
				}else{
					if(exp[2].equals(values[column_position-1]))
						check = true;
					else
						check = false;
			}
		}
		return check;
	}


public static void filter(RandomAccessFile file, String[] exp, String[] column_name, disp disp)
	{
		try{
			
			int num_of_pages = Base.num_pages(file);
			for(int page = 1; page <= num_of_pages; page++){
				
				file.seek((page-1)*page_size);
				byte page_type = file.readByte();
				if(page_type == 0x0D)
				{
					byte num_of_cells = Btree.getCellNo(file, page);

					for(int i=0; i < num_of_cells; i++){
						
						long location = Btree.getCellLocation(file, page, i);	
						String[] values = Base.retrieve_data(file, location);
						int rowid=Integer.parseInt(values[0]);

						boolean check = expressionVal(values, rowid, exp, column_name);
						
						if(check)
							disp.add(rowid, values);
					}
				}
				else
					continue;
			}

			disp.columnName = column_name;
			disp.format = new int[column_name.length];

		}catch(Exception e){
			System.out.println("Error at filter");
			e.printStackTrace();
		}

	}

	public static void filter(RandomAccessFile file, String[] exp, String[] column_name, String[] type, disp disp)
	{
		try{
			
			int num_of_pages = Base.num_pages(file);
			
			for(int page = 1; page <= num_of_pages; page++){
				
				file.seek((page-1)*page_size);
				byte page_type = file.readByte();
				
					if(page_type == 0x0D){
						
					byte numOfCells = Btree.getCellNo(file, page);

					 for(int i=0; i < numOfCells; i++){
						long location = Btree.getCellLocation(file, page, i);
						String[] values = Base.retrieve_data(file, location);
						int rowid=Integer.parseInt(values[0]);
						
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								values[j] = "'"+values[j]+"'";
						
						boolean check = expressionVal(values, rowid , exp, column_name);

						
						for(int j=0; j < type.length; j++)
							if(type[j].equals("DATE") || type[j].equals("DATETIME"))
								values[j] = values[j].substring(1, values[j].length()-1);

						if(check)
							disp.add(rowid, values);
					 }
				   }
				    else
						continue;
			}

			disp.columnName = column_name;
			disp.format = new int[column_name.length];

		}catch(Exception e){
			System.out.println("Error at filter");
			e.printStackTrace();
		}

	}

		public static String[] get_data_type(String table)
	{
		String[] data_type = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			disp disp = new disp();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable"};
			String[] exp = {"table_name","=",table};
			filter(file, exp, columnName, disp);
			HashMap<Integer, String[]> content = disp.content;
			ArrayList<String> arr = new ArrayList<String>();
			for(String[] v : content.values()){
				arr.add(v[3]);
			}
			int size=arr.size();
			data_type = arr.toArray(new String[size]);
			file.close();
			return data_type;
		}catch(Exception e){
			System.out.println(e);
		}
		return data_type;
	}


		public static int calculate_payload(String table, String[] values, byte[] type_of_data)
	{
		String[] dataType = get_data_type(table);
		int size =dataType.length;
		for(int i = 1; i < dataType.length; i++){
			type_of_data[i - 1]= get_type_of_data(values[i], dataType[i]);
			size = size + field_length(type_of_data[i - 1]);
		}
		return size;
	}

public static String[] get_nullable(String table)
	{
		String[] nullable = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			disp disp = new disp();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable"};
			String[] exp = {"table_name","=",table};
			filter(file, exp, columnName, disp);
			HashMap<Integer, String[]> content = disp.content;
			ArrayList<String> arr = new ArrayList<String>();
			for(String[] v : content.values()){
				arr.add(v[5]);
			}
			int size=arr.size();
			nullable = arr.toArray(new String[size]);
			file.close();
			return nullable;
		}catch(Exception e){
			System.out.println(e);
		}
		return nullable;
	}


	public static String[] retrieve_data(RandomAccessFile file, long location)
	{
		
		String[] values = null;
		try{
			
			SimpleDateFormat date_format = new SimpleDateFormat (date_pattern);

			file.seek(location+2);
			int row_id = file.readInt();
			int num_columns = file.readByte();
			
			byte[] type_of_data = new byte[num_columns];
			file.read(type_of_data);
			
			values = new String[num_columns+1];
			
			values[0] = Integer.toString(row_id);
			
			for(int i=1; i <= num_columns; i++){
				switch(type_of_data[i-1]){
					case 0x00:  file.readByte();
					            values[i] = "null";
								break;

					case 0x01:  file.readShort();
					            values[i] = "null";
								break;

					case 0x02:  file.readInt();
					            values[i] = "null";
								break;

					case 0x03:  file.readLong();
					            values[i] = "null";
								break;

					case 0x04:  values[i] = Integer.toString(file.readByte());
								break;

					case 0x05:  values[i] = Integer.toString(file.readShort());
								break;

					case 0x06:  values[i] = Integer.toString(file.readInt());
								break;

					case 0x07:  values[i] = Long.toString(file.readLong());
								break;

					case 0x08:  values[i] = String.valueOf(file.readFloat());
								break;

					case 0x09:  values[i] = String.valueOf(file.readDouble());
								break;

					case 0x0A:  Long temp_date = file.readLong();
								Date date_time = new Date(temp_date);
								values[i] = date_format.format(date_time);
								break;

					case 0x0B:  temp_date = file.readLong();
								Date date = new Date(temp_date);
								values[i] = date_format.format(date).substring(0,10);
								break;

					default:    int len = new Integer(type_of_data[i-1]-0x0C);
								byte[] bytes = new byte[len];
								file.read(bytes);
								values[i] = new String(bytes);
								break;
				}
			}

		}catch(Exception e){
			System.out.println(e);
		}

		return values;
	}

}