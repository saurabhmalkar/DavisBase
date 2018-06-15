import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class DDL{

public static int page_size = 512;



	public static void showTables() {
		System.out.println("STUB: Calling the method to process the command");
		System.out.println("Parsing the string:\"show tables\"");
		
		String table = "davisbase_tables";
		String[] columns = {"table_name"};
		String[] compute = new String[0];
		VDL.select(table, columns, compute);
	}



	public static void createTable(String table, String[] columns)
	{
		try{	
			
			RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
			file.setLength(page_size);
			file.seek(0);
			file.writeByte(0x0D);
			file.close();
				
			file = new RandomAccessFile("data/davisbase_tables.tbl", "rw");
				
			int num_of_pages = Base.num_pages(file);
			int page=1;
			for(int p = 1; p <= num_of_pages; p++){
				int right_most = Btree.get_right_most(file, p);
				if(right_most == 0)
					page = p;
			}
				
			int[] keys = Btree.get_key_array(file, page); //obtain all the keys in the table
			int l = keys[0];
			for(int i = 0; i < keys.length; i++) //find the max key
				if(keys[i]>l)
					l = keys[i];
			file.close();
				
			String[] values = {Integer.toString(l+1), table};
			DML.insert_into("davisbase_tables", values);

			file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			
			num_of_pages = Base.num_pages(file);
			page=1;
			for(int p = 1; p <= num_of_pages; p++){
				int right_most = Btree.get_right_most(file, p);
				if(right_most == 0)
					page = p;
			}
				
			keys = Btree.get_key_array(file, page);
			l = keys[0];
			for(int i = 0; i < keys.length; i++)
				if(keys[i]>l)
					l = keys[i];
			file.close();

			for(int i = 0; i < columns.length; i++){
				l = l + 1;
				String[] tokens = columns[i].split(" ");
				String column_name = tokens[0];
				String token_data = tokens[1].toUpperCase();
				String position = Integer.toString(i+1);
				String nullable;
				if(tokens.length > 2)
					nullable = "NO";
				else
					 nullable = "YES";
				String[] value = {Integer.toString(l), table, column_name, token_data, position, nullable};
				DML.insert_into("davisbase_columns", value);
			}
		
		}catch(Exception e){
			System.out.println(e);
		}
	}






	public static void drop(String table)
	{
		try{
			
			RandomAccessFile file = new RandomAccessFile("data/davisbase_tables.tbl", "rw");
			int num_of_pages = Base.num_pages(file);
			for(int page = 1; page <= num_of_pages; page ++){
				file.seek((page-1)*page_size);
				byte file_type = file.readByte();
				if(file_type == 0x0D)
				{
					short[] cells_address = Btree.get_cell_array(file, page);
					int j = 0;
					for(int i = 0; i < cells_address.length; i++)
					{
						long location = Btree.getCellLocation(file, page, i);
						String[] values = Base.retrieve_data(file, location);
						String table_name = values[1];
						if(!table_name.equals(table))
						{
							Btree.set_cell_offset(file, page, j, cells_address[i]);
							j++;
						}
					}
					Btree.setCellNo(file, page, (byte)j);
				}
				else
					continue;
			}
			file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			num_of_pages = Base.num_pages(file);
			for(int page = 1; page <= num_of_pages; page ++){
				file.seek((page-1)*page_size);
				byte file_type = file.readByte();
				if(file_type == 0x0D)
				{
					short[] cells_address = Btree.get_cell_array(file, page);
					int k = 0;
					for(int i = 0; i < cells_address.length; i++)
					{
						long location = Btree.getCellLocation(file, page, i);
						String[] values = Base.retrieve_data(file, location);
						String table_name = values[1];
						if(!table_name.equals(table))
						{
							Btree.set_cell_offset(file, page, k, cells_address[i]);
							k++;
						}
					}
					Btree.setCellNo(file, page, (byte)k);
				}
				else
					continue;
			}

			File oldFile = new File("data", table+".tbl"); 
			oldFile.delete();
		}catch(Exception e){
			System.out.println(e);
		}

	}

	public static void dropTable(String dropTableString) {
		System.out.println("STUB: Calling the method to process the command");
		System.out.println("Parsing the string:\"" + dropTableString + "\"");
		
		String[] tokens=dropTableString.split(" ");
		String table_name = tokens[2];
		if(!Base.isTableExists(table_name)){
			System.out.println("Table "+table_name+" does not exist.");
		}
		else
		{
			drop(table_name);
		}		

	}



}