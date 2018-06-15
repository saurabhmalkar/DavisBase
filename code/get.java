import java.io.RandomAccessFile;
public class get{

	public static int page_size = 512;
		public static byte get_type_of_page(RandomAccessFile file, int page)
	{
		byte type=0x05;
		try {
			file.seek((page-1)*page_size);
			type = file.readByte();
		} catch (Exception e) {
			System.out.println(e);
		}
		return type;
	}

		public static boolean hasRowId(RandomAccessFile file, int page, int rowid)
	{
		int[] keys = Btree.get_key_array(file, page);
		for(int i : keys)
			if(rowid == i)
				return true;
		return false;
	}



	public static void main(String[] args){}
}