import java.io.RandomAccessFile;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Btree{
	public static int page_size = 512;
	public static final String date_pattern = "yyyy-MM-dd_HH:mm:ss";



	public static int find_midkey(RandomAccessFile file, int page){         //findMidKey
		int value = 0;
		try{
			file.seek((page-1)*page_size);
			byte page_type = file.readByte();
			int no_cells = getCellNo(file, page);
			int middle = (int) Math.ceil((double) no_cells / 2);
			long location = getCellLocation(file, page, middle-1);
			file.seek(location);

			switch(page_type){
				case 0x05:
					file.readInt(); 
					value = file.readInt();
					break;
				case 0x0D:
					file.readShort();
					value = file.readInt();
					break;
				}
			}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}

	public static int Intpage_create(RandomAccessFile file)
	{
		int no_pages = 0;
		try{
			no_pages = (int)(file.length()/(new Long(page_size)));
			no_pages = no_pages + 1;
			file.setLength(page_size * no_pages);
			file.seek((no_pages-1)*page_size);
			file.writeByte(0x05); 
		}catch(Exception e){
			System.out.println(e);
		}

		return no_pages;
	}

	public static int leafpage_create(RandomAccessFile file)
	{
		int no_pages = 0;
		try{
			no_pages = (int)(file.length()/(new Long(page_size)));
			no_pages = no_pages + 1;
			file.setLength(page_size * no_pages);
			file.seek((no_pages-1)*page_size);
			file.writeByte(0x0D); 
		}catch(Exception e){
			System.out.println(e);
		}

		return no_pages;

	}



	public static void int_page_split(RandomAccessFile file, int current_page, int new_page)
	{
		try{
			
			int no_cells = getCellNo(file, current_page);
			
			int middle = (int) Math.ceil((double) no_cells / 2);

			int cell_num_A = middle - 1;
			int cell_num_B = no_cells - cell_num_A - 1;
			short content = 512;

			for(int i = cell_num_A+1; i < no_cells; i++){
				long location = getCellLocation(file, current_page, i);
				short cell_size = 8;
				content = (short)(content - cell_size);
				file.seek(location);
				byte[] new_cell = new byte[cell_size];
				file.read(new_cell);
				file.seek((new_page-1)*page_size+content);
				file.write(new_cell);
				file.seek(location);
				int page = file.readInt();
				set_parent(file, page, new_page);
				set_cell_offset(file, new_page, i - (cell_num_A + 1), content);
			}
			
			int temp = get_right_most(file, current_page);
			set_right_most(file, new_page, temp);
			
			long middle_location = getCellLocation(file, current_page, middle - 1);
			file.seek(middle_location);
			temp = file.readInt();
			set_right_most(file, current_page, temp);
			
			file.seek((new_page-1)*page_size+2);
			file.writeShort(content);
			
			short off = get_cell_offset(file, current_page, cell_num_A-1);
			file.seek((current_page-1)*page_size+2);
			file.writeShort(off);

			
			int parent = get_parent(file, current_page);
			set_parent(file, new_page, parent);
			
			byte number = (byte) cell_num_A;
			setCellNo(file, current_page, number);
			number = (byte) cell_num_B;
			setCellNo(file, new_page, number);
			
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public static void leafpage_split(RandomAccessFile file, int current_page, int new_page)
	{
		try{
			
			int no_cells = getCellNo(file, current_page);
			
			int middle = (int) Math.ceil((double) no_cells / 2);

			int cell_num_A = middle - 1;
			int cell_num_B = no_cells - cell_num_A;
			int content = 512;

			for(int i = cell_num_A; i < no_cells; i++){
				long location = getCellLocation(file, current_page, i);
				file.seek(location);
				int cell_size = file.readShort()+6;
				content = content - cell_size;
				file.seek(location);
				byte[] new_cell = new byte[cell_size];
				file.read(new_cell);
				file.seek((new_page-1)*page_size+content);
				file.write(new_cell);
				set_cell_offset(file, new_page, i - cell_num_A, content);
			}

			
			file.seek((new_page-1)*page_size+2);
			file.writeShort(content);

			
			short off = get_cell_offset(file, current_page, cell_num_A-1);
			file.seek((current_page-1)*page_size+2);
			file.writeShort(off);

			
			int right_most = get_right_most(file, current_page);
			set_right_most(file, new_page, right_most);
			set_right_most(file, current_page, new_page);

			
			int parent = get_parent(file, current_page);
			set_parent(file, new_page, parent);

			
			byte number = (byte) cell_num_A;
			setCellNo(file, current_page, number);
			number = (byte) cell_num_B;
			setCellNo(file, new_page, number);
			
		}catch(Exception e){
			System.out.println(e);
			
		}
	}

	
	public static int int_node_split(RandomAccessFile file, int page)
	{
		int new_page = Intpage_create(file);
		int middle_key = find_midkey(file, page);
		int_page_split(file, page, new_page);
		int parent = get_parent(file, page);
		if(parent == 0){
			int root_page = Intpage_create(file);
			set_parent(file, page, root_page);
			set_parent(file, new_page, root_page);
			set_right_most(file, root_page, new_page);
			insert_cell_node(file, root_page, page, middle_key);
			return root_page;
		}else{
			long pointer_location = get_location(file, page, parent);
			set_location(file, pointer_location, parent, new_page);
			insert_cell_node(file, parent, page, middle_key);
			array_sort(file, parent);
			return parent;
		}
	}

	public static void leaf_split(RandomAccessFile file, int page)
	{
		int new_page = leafpage_create(file);
		int middle_key = find_midkey(file, page);
		leafpage_split(file, page, new_page);
		int parent = get_parent(file, page);
		if(parent == 0){
			int root_page = Intpage_create(file);
			set_parent(file, page, root_page);
			set_parent(file, new_page, root_page);
			set_right_most(file, root_page, new_page);
			insert_cell_node(file, root_page, page, middle_key);
		}else{
			long pointer_location = get_location(file, page, parent);
			set_location(file, pointer_location, parent, new_page);
			insert_cell_node(file, parent, page, middle_key);
			array_sort(file, parent);
			while(checkSpaceInInteriorNode(file, parent)){
				parent = int_node_split(file, parent);
			}
		}
	}


//sortCellArray
	public static void array_sort(RandomAccessFile file, int page){
		 byte number = getCellNo(file, page);
		 int[] key_array = get_key_array(file, page);
		 short[] cell_array = get_cell_array(file, page);
		 int l_temp;
		 short r_temp;

		 for (int i = 1; i < number; i++) {
            for(int j = i ; j > 0 ; j--){
                if(key_array[j] < key_array[j-1]){

                	r_temp = (short) key_array[j];
                    key_array[j] = key_array[j-1];
                    key_array[j-1] = r_temp;
                	
                    l_temp = key_array[j];
                    key_array[j] = key_array[j-1];
                    key_array[j-1] = l_temp;

                }
            }
         }

         try{
         	file.seek((page-1)*page_size+12);
         	for(int i = 0; i < number; i++){
				file.writeShort(key_array[i]);
			}
         }catch(Exception e){
       
         }
	}



	public static int[] get_key_array(RandomAccessFile file, int page)
	{
		int number = new Integer(getCellNo(file, page));
		int[] arr = new int[number];

		try{
			file.seek((page-1)*page_size);
			byte page_type = file.readByte();
			byte offset = 0;
			switch(page_type){
			    case 0x0d:
				    offset = 2;
				    break;
				case 0x05:
					offset = 4;
					break;
				default:
					offset = 2;
					break;
			}

			for(int i = 0; i < number; i++){
				long location = getCellLocation(file, page, i);
				file.seek(location+offset);
				arr[i] = file.readInt();
			}

		}catch(Exception e){
			System.out.println(e);
		}

		return arr;
	}




	public static short[] get_cell_array(RandomAccessFile file, int page)
	{
		int number = new Integer(getCellNo(file, page));
		short[] arr = new short[number];

		try{
			file.seek((page-1)*page_size+12);
			for(int i = 0; i < number; i++){
				arr[i] = file.readShort();
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return arr;
	}


	public static long get_location(RandomAccessFile file, int page, int parent)
	{
		long value = 0;
		try{
			int no_cells = new Integer(getCellNo(file, parent));
			for(int i=0; i < no_cells; i++){
				long location = getCellLocation(file, parent, i);
				file.seek(location);
				int childPage = file.readInt();
				if(childPage == page){
					value = location;
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}




	public static void set_location(RandomAccessFile file, long location, int parent, int page)
	{
		try{
			if(location == 0){
				file.seek((parent-1)*page_size+4);
			}else{
				file.seek(location);
			}
			file.writeInt(page);
		}catch(Exception e){
			System.out.println(e);
		}
	} 



	
	public static void insert_cell_node(RandomAccessFile file, int page, int child, int rowid)
	{
		try{
			
			file.seek((page-1)*page_size+2);
			short content = file.readShort();
			
			if(content == 0)
				content = 512;
			
			content = (short)(content - 8);
			
			file.seek((page-1)*page_size+content);
			file.writeInt(child);
			file.writeInt(rowid);
			
			file.seek((page-1)*page_size+2);
			file.writeShort(content);
			
			byte number = getCellNo(file, page);
			set_cell_offset(file, page ,number, content);
			
			number = (byte) (number + 1);
			setCellNo(file, page, number);

		}catch(Exception e){
			System.out.println(e);
		}
	}




	public static void insert_cell_leaf(RandomAccessFile file, int page, int offset, short payload_size, int rowid, byte[] type_of_data, String[] values)
	{
		try{
			String str;
			file.seek((page-1)*page_size+offset);
			file.writeShort(payload_size);
			file.writeInt(rowid);
			int column = values.length - 1;
			file.writeByte(column);
			file.write(type_of_data);
			for(int i = 1; i < values.length; i++){
				switch(type_of_data[i-1]){
					case 0x00:
						file.writeByte(0);
						break;
					case 0x01:
						file.writeShort(0);
						break;
					case 0x02:
						file.writeInt(0);
						break;
					case 0x03:
						file.writeLong(0);
						break;
					case 0x04:
						file.writeByte(new Byte(values[i]));
						break;
					case 0x05:
						file.writeShort(new Short(values[i]));
						break;
					case 0x06:
						file.writeInt(new Integer(values[i]));
						break;
					case 0x07:
						file.writeLong(new Long(values[i]));
						break;
					case 0x08:
						file.writeFloat(new Float(values[i]));
						break;
					case 0x09:
						file.writeDouble(new Double(values[i]));
						break;
					case 0x0A:
						str = values[i];
						Date temp_date = new SimpleDateFormat(date_pattern).parse(str.substring(1, str.length()-1));
						long time = temp_date.getTime();
						file.writeLong(time);
						break;
					case 0x0B:
						str = values[i];
						str = str.substring(1, str.length()-1);
						str = str+"_00:00:00";
						Date temp_date2 = new SimpleDateFormat(date_pattern).parse(str);
						long time2 = temp_date2.getTime();
						file.writeLong(time2);
						break;
					default:
						file.writeBytes(values[i]);
						break;
				}
			}
			int cell_num = getCellNo(file, page);
			byte temp = (byte) (cell_num+1);
			setCellNo(file, page, temp);
			file.seek((page-1)*page_size+12+cell_num*2);
			file.writeShort(offset);
			file.seek((page-1)*page_size+2);
			int content = file.readShort();
			if(content >= offset || content == 0){
				file.seek((page-1)*page_size+2);
				file.writeShort(offset);
			}
		}catch(Exception e){
			System.out.println(e);
		}
	}




	public static void update_cell_leaf(RandomAccessFile file, int page, int offset, int payload_size, int rowid, byte[] type_of_data, String[] values)
	{
		try{
			String str;
			file.seek((page-1)*page_size+offset);
			file.writeShort(payload_size);
			file.writeInt(rowid);
			int column = values.length - 1;
			file.writeByte(column);
			file.write(type_of_data);
			for(int i = 1; i < values.length; i++){
				switch(type_of_data[i-1]){
					case 0x00:
						file.writeByte(0);
						break;
					case 0x01:
						file.writeShort(0);
						break;
					case 0x02:
						file.writeInt(0);
						break;
					case 0x03:
						file.writeLong(0);
						break;
					case 0x04:
						file.writeByte(new Byte(values[i]));
						break;
					case 0x05:
						file.writeShort(new Short(values[i]));
						break;
					case 0x06:
						file.writeInt(new Integer(values[i]));
						break;
					case 0x07:
						file.writeLong(new Long(values[i]));
						break;
					case 0x08:
						file.writeFloat(new Float(values[i]));
						break;
					case 0x09:
						file.writeDouble(new Double(values[i]));
						break;
					case 0x0A:
						str = values[i];
						Date temp_date = new SimpleDateFormat(date_pattern).parse(str.substring(1, str.length()-1));
						long time = temp_date.getTime();
						file.writeLong(time);
						break;
					case 0x0B:
						str = values[i];
						str = str.substring(1, str.length()-1);
						str = str+"_00:00:00";
						Date temp_date2 = new SimpleDateFormat(date_pattern).parse(str);
						long time2 = temp_date2.getTime();
						file.writeLong(time2);
						break;
					default:
						file.writeBytes(values[i]);
						break;
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}
	}


	public static int check_space_leaf(RandomAccessFile file, int page, int size)
	{
		int value = -1;

		try{
			file.seek((page-1)*page_size+2);
			int content = file.readShort();
			if(content == 0)
				return page_size - size;
			int no_cells = getCellNo(file, page);
			int space = content - 20 - 2*no_cells;
			if(size < space)
				return content - size;
			
		}catch(Exception e){
			System.out.println(e);
		}

		return value;
	}




	public static void setCellNo(RandomAccessFile file, int page, byte number)
	{
		try{
			file.seek((page-1)*page_size+1);
			file.writeByte(number);
		}catch(Exception e){
			System.out.println(e);
		}
	}




	public static byte getCellNo(RandomAccessFile file, int page)
	{
		byte value = 0;
		try{
			file.seek((page-1)*page_size+1);
			value = file.readByte();
		}catch(Exception e){
			System.out.println(e);
		}
		return value;
	}
//checkInteriorSpace	
	public static boolean checkSpaceInInteriorNode(RandomAccessFile file, int page)
	{
		byte no_cells = getCellNo(file, page);
		if(no_cells > 30)
			return true;
		else
			return false;
	}

	


	public static int get_right_most(RandomAccessFile file, int page)
	{
		int right_most = 0;

		try{
			file.seek((page-1)*page_size+4);
			right_most = file.readInt();
		}catch(Exception e){
			System.out.println("Error at get_right_most");
		}

		return right_most;
	}




	public static void set_right_most(RandomAccessFile file, int page, int rightLeaf)
	{

		try{
			file.seek((page-1)*page_size+4);
			file.writeInt(rightLeaf);
		}catch(Exception e){
			System.out.println("Error at set_right_most");
		}

	}




	public static int get_parent(RandomAccessFile file, int page)
	{
		int value = 0;

		try{
			file.seek((page-1)*page_size+8);
			value = file.readInt();
		}catch(Exception e){
			System.out.println(e);
		}
		return value;
	}





	public static void set_parent(RandomAccessFile file, int page, int parent)
	{
		try{
			file.seek((page-1)*page_size+8);
			file.writeInt(parent);
		}catch(Exception e){
			System.out.println(e);
		}
	}


	
	public static long getCellLocation(RandomAccessFile file, int page, int id)
	{
		long location = 0;
		try{
			file.seek((page-1)*page_size+12+id*2);
			short off = file.readShort();
			long origin = (page-1)*page_size;
			location = origin + off;
		}catch(Exception e){
			System.out.println(e);
		}
		return location;
	}





	public static short get_cell_offset(RandomAccessFile file, int page, int id)
	{
		short off = 0;
		try{
			file.seek((page-1)*page_size+12+id*2);
			off = file.readShort();
		}catch(Exception e){
			System.out.println(e);
		}
		return off;
	}



	public static void set_cell_offset(RandomAccessFile file, int page, int id, int off)
	{
		try{
			file.seek((page-1)*page_size+12+id*2);
			file.writeShort(off);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void main(String[] args){}
}
