import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DML{



	public static int page_size = 512;
	public static void main(String[] args){}


	public static String[] getColumnName(String table)
	{
		String[] columns = new String[0];
		try{
			RandomAccessFile file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			disp disp = new disp();
			String[] columnName = {"rowid", "table_name", "column_name", "data_type", "ordinal_position", "is_nullable"};
			String[] exp = {"table_name","=",table};
			Base.filter(file, exp, columnName, disp);
			HashMap<Integer, String[]> content = disp.content;
			ArrayList<String> array = new ArrayList<String>();
			for(String[] i : content.values()){
				array.add(i[2]);
			}
			int size=array.size();
			columns = array.toArray(new String[size]);
			file.close();
			return columns;
		}catch(Exception e){
			System.out.println(e);
		}
		return columns;
	}





    public static int searchPageWithKey(RandomAccessFile file, int key)
    {
		int value = 1;
		try{
			int num_of_pages = Base.num_pages(file);
			for(int page = 1; page <= num_of_pages; page++){
				file.seek((page - 1)*page_size);
				byte page_type = file.readByte();
				if(page_type == 0x0D){
					int[] keys = Btree.get_key_array(file, page);
					if(keys.length == 0)
						return 0;
					int right_most = Btree.get_right_most(file, page);
					if(keys[0] <= key && key <= keys[keys.length - 1]){
						return page;
					}else if(right_most == 0 && keys[keys.length - 1] < key){
						return page;
					}
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}

		public static void insert_into(String table, String[] values){
		try{
			RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
			insert_into_file(file, table, values);
			file.close();

		}catch(Exception e){
			System.out.println(e);
		}
	}



public static void insert_into_file(RandomAccessFile file, String table, String[] values)
	{
		String[] data_type = Base.get_data_type(table);
		String[] nullable = Base.get_nullable(table);

		for(int i = 0; i < nullable.length; i++)
			if(values[i].equals("null") && nullable[i].equals("NO")){
				System.out.println("NULL-value constraint violation");
				System.out.println();
				return;
			}

		int row_id = new Integer(values[0]);
		int page = searchPageWithKey(file, row_id);
		if(page != 0)
			if(get.hasRowId(file, page, row_id)){
				System.out.println("Uniqueness constraint violation");
				return;
			}
		if(page == 0)
			page = 1;


		byte[] type_of_data = new byte[data_type.length-1];
		short payload_size = (short) Base.calculate_payload(table, values, type_of_data);
		int cell_size = payload_size + 6;
		int off = Btree.check_space_leaf(file, page, cell_size);


		if(off != -1){
			Btree.insert_cell_leaf(file, page, off, payload_size, row_id, type_of_data, values);
		}else{
			Btree.leaf_split(file, page);
			insert_into_file(file, table, values);
		}
	}

	public static void delete(String table, String[] exp)
	{
		try{
		int key = new Integer(exp[2]);
		RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
		int num_of_pages = Base.num_pages(file);
		int page = 0;
		for(int p = 1; p <= num_of_pages; p++)
			if(get.hasRowId(file, p, key)&get.get_type_of_page(file, p)==0x0D){
				page = p;
				break;
			}
		
		if(page==0)
		{
			System.out.println("The given key value does not exist");
			return;
		}
			
			short[] cells_address = Btree.get_cell_array(file, page);
			int k = 0;
			for(int i = 0; i < cells_address.length; i++)
			{
				long location = Btree.getCellLocation(file, page, i);
				String[] values = Base.retrieve_data(file, location);
				int j = new Integer(values[0]);
				if(j!=key)
				{
					Btree.set_cell_offset(file, page, k, cells_address[i]);
					k++;
				}
			}
			Btree.setCellNo(file, page, (byte)k);
			
		}catch(Exception e)
		{
			System.out.println(e);
		}
		
	}




}