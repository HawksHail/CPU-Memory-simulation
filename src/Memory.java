
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author Zion Mantey
 */
public class Memory {

    private final int RAM[];

    public Memory() {
        RAM = new int[2000];
        //Arrays.fill(RAM, -1);
    }

    /**
     * Loads filename into RAM
     */
    private void init(String filename) throws FileNotFoundException {
        Scanner inFile = new Scanner(new File(filename));
        int i = 0;
        while (inFile.hasNextLine()) {
            String[] line = inFile.nextLine().trim().split("\\s+"); //trim to remove whitespace on ends, then split on whitespace
            //System.out.println(Arrays.toString(line));   .
            if (line[0].length() > 0) {             //if line is not empty
                if (line[0].matches("-?\\d+")) {    //if first element is a number insert into RAM
                    RAM[i++] = Integer.valueOf(line[0]);
                } else if (line[0].charAt(0) == '.') {  //if first char is a period, move i
                    i = Integer.valueOf(line[0].substring(1));
                }
            }
        }
        inFile.close();
    }

    /**
     *
     * @param address address to read
     * @return data at address
     */
    private int read(int address) {
        return RAM[address];
    }

    /**
     *
     * @param address address to read
     * @param data data to write at address
     * @param kernelMode if the CPU is in kernelMode, access to reserved space is granted
     * @return if successful
     */
    private void write(int address, int data) {
        RAM[address] = data;
    }

    /**
     * Simulates basic memory. Format is "command address data"
     *
     * @param args filename
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        //System.out.println("Memory main");
        Memory mem = new Memory();
        mem.init(args[0]);
        boolean done = false;
        Scanner in = new Scanner(System.in);
        String line[];
        while (!done) {
            //command address data
            line = in.nextLine().trim().toLowerCase().split("\\s+");
            //System.out.println(Arrays.toString(line));
            switch (line[0]) {
                case "read":
                    System.out.println(mem.read(Integer.valueOf(line[1])));
                    break;

                case "write":
                    mem.write(Integer.valueOf(line[1]), Integer.valueOf(line[2]));
                    break;

                case "stop":
                default:
                    done = true;
            }

        }
    }

}
