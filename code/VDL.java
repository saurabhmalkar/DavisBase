import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;





public class VDL{


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



		public static void select(String table, String[] columns, String[] exp)
	{
		try{
			
			RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
			String[] column_name = getColumnName(table);
			String[] type = Base.get_data_type(table);
			
			disp disp = new disp();
			
			Base.filter(file, exp, column_name, type, disp);
			disp.display(columns);
			file.close();
		}catch(Exception e){
			System.out.println(e);
		}
	}


}