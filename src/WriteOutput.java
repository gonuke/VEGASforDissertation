import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class WriteOutput {


	private String path;
	private boolean append_to_file = false;
	private boolean add_to_file = true;
	
	public WriteOutput(String file_path) {
		path = file_path;
	}
	
	public WriteOutput(String file_path, boolean append_value) {
		path = file_path;
		append_to_file = append_value;
	}
	
	public void writeToFile(double output) throws IOException{
		FileWriter write = new FileWriter(path, append_to_file);
		PrintWriter print = new PrintWriter(write);
		print.printf("%s" + "\t", output);
		print.close();
	}
	
	public void appendToFile(double output) throws IOException{
		FileWriter write = new FileWriter(path, add_to_file);
		PrintWriter print = new PrintWriter(write);
		print.printf("%s" + "\t", output);
		print.close();
	}
	
	public void appendToFile(boolean output) throws IOException{
		FileWriter write = new FileWriter(path, add_to_file);
		PrintWriter print = new PrintWriter(write);
		print.printf("%s" + "\t", output);
		print.close();
	}
	
	public void appendNewLine() throws IOException{
		FileWriter write = new FileWriter(path, add_to_file);
		PrintWriter print = new PrintWriter(write);
		print.printf("%n");
		print.close();
	}
	
	
}