import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class MergeFiles {

	public static void main(String[] args) {
		String dir = "/home/sida/liyu/github/";
		String filePath1 = dir + "test1.txt";
		String filePath2 = dir + "test2.txt";
		String filePath3 = dir + "test3.txt";
		String outFilePath = dir + "results.txt";
		String action = "f";
		update(filePath1,filePath2,filePath3,outFilePath,action);
	}

	public static void update(String filePath1, String filePath2, String filePath3,
														String outFilePath, String action) {

		File[] files = new File[3];
		File outFile = null;
		try {
			files[0] = new File(filePath1);
			files[1] = new File(filePath2);
			files[2] = new File(filePath3);
			outFile = new File(outFilePath);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		try {
			outFile.delete();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		addContents(files[0],files[1],action);
		mergeFiles(files,outFile);
	}


	public static void addContents(File f1, File f2, String action) {
		String toBeAdded = String.format("<http://www.semanticweb.org/rui/ontologies/2016/5/untitled-ontology-31#%s> a owl:NamedIndividual .\n",action);
		try
		{
				FileWriter fw1 = new FileWriter(f1,true); //the true will append the new data
				FileWriter fw2 = new FileWriter(f2,true); //the true will append the new data
				fw1.write(toBeAdded);//appends the string to the file
				fw2.write(toBeAdded);
				fw1.close();
				fw2.close();
		}
		catch(IOException ioe)
		{
				System.err.println("IOException: " + ioe.getMessage());
		}
	}

	public static void mergeFiles(File[] files, File mergedFile) {

		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			 out = new BufferedWriter(fstream);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		for (File f : files) {
			System.out.println("merging: " + f.getName());
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
				BufferedReader in = new BufferedReader(new InputStreamReader(fis));

				String aLine;
				while ((aLine = in.readLine()) != null) {
					out.write(aLine);
					out.newLine();
				}

				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



}
