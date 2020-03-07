
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A program that simulates a primitive CPU and memory system
 *
 * @author Zion Mantey
 */
public class CPU {

    private boolean kernelMode = false;
    private int PC = 0, SP = 1000, IR = 0, AC = 0, X = 0, Y = 0, timer = 0;
    private final int TIMER_MAX;
    private static Process mem;
    private Scanner memIn;
    private PrintWriter memOut;
    private boolean done = false;
    private static boolean debug = false;

    /**
     * Initializes timer
     *
     * @param timer_max maximum value for timer
     */
    private CPU(int timer_max) {
        TIMER_MAX = timer_max;
    }

    /**
     * Initializes timer to INT_MAX
     */
    private CPU() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Reads memory at PC and stores it in IR then increments PC
     */
    public void fetch() {
        IR = fetch(PC);
        ++PC;
    }

    /**
     * Reads memory at address and returns it
     *
     * @param address Address to read at
     * @return Data at address
     */
    public int fetch(int address) {
        if (address < 1000 || (address >= 1000 && kernelMode)) {    //if non-protected region or protected region with kernelMode
            //command address data
            if (debug) {
                System.out.print("READ   " + address + " ");
            }
            memOut.println("read " + address);
            memOut.flush();
            String line = memIn.nextLine();
            if (debug) {
                System.out.println(line);
            }
            return Integer.valueOf(line);
        } else {
            done = true;
            System.out.println("Memory violation: accessing system address " + address + " in user mode ");
            return -1;
        }
    }

    /**
     * Writes data to memory at address
     *
     * @param address Address to write to
     * @param data Data to write to address
     */
    public void write(int address, int data) {
        //command address data
        //System.out.println(address + " write");
        if (debug) {
            System.out.println("WRITE  " + address + " " + data);
        }
        memOut.println("write " + address + " " + data);
        memOut.flush();
    }

    /**
     * Executes the instruction in IR register
     */
    public void execute() {
        //System.out.println(IR + " exec");
        switch (IR) {
            case 1://load value
                fetch();
                AC = IR;
                break;

            case 2: //load addr
                fetch();
                AC = fetch(IR);
                break;

            case 3: //loadInd addr
                fetch();
                //System.out.println("loadInd");
                AC = fetch(fetch(IR));
                break;

            case 4: //loadIdxX addr
                fetch();
                //System.out.println("loadIdxX");
                AC = fetch(IR + X);
                break;

            case 5: //loadIdxY addr
                fetch();
                //System.out.println("loadIdxY");
                AC = fetch(IR + Y);
                break;

            case 6: //loadSpX    
                //System.out.println("loadSpX");
                AC = fetch(SP + X);
                break;

            case 7: //store addr
                fetch();
                write(IR, AC);
                break;

            case 8: //get                
                AC = (new Random().nextInt(100) + 1);
                break;

            case 9: //put port                
                fetch();
                if (IR == 1) {
                    System.out.print(AC);
                } else {
                    System.out.print((char) AC);
                }
                break;

            case 10: //add x
                AC += X;
                break;

            case 11: // add y
                AC += Y;
                break;

            case 12: //sub x
                AC -= X;
                break;

            case 13: //sub y
                AC -= Y;
                break;

            case 14: //copy to x
                X = AC;
                break;

            case 15: //copy from x
                AC = X;
                break;

            case 16: // copy to y
                Y = AC;
                break;

            case 17: // copy from y
                AC = Y;
                break;

            case 18: // copy to SP
                SP = AC;
                break;

            case 19: // copy from SP
                AC = SP;
                break;

            case 20: //jump addr
                fetch();
                PC = IR;
                break;

            case 21: //jump if equal addr
                fetch();
                if (AC == 0) {
                    PC = IR;
                }
                break;

            case 22: // jump if not equal addr
                fetch();
                if (AC != 0) {
                    PC = IR;
                }
                break;

            case 23: //call addr
                fetch();
                --SP;
                write(SP, PC);
                PC = IR;
                break;

            case 24: //return                
                PC = fetch(SP);
                ++SP;
                break;

            case 25: // inc x
                ++X;
                break;

            case 26: //dec x
                --X;
                break;

            case 27: //push AC onto stack
                --SP;
                write(SP, AC);
                break;

            case 28: //pop stack to AC
                AC = fetch(SP);
                ++SP;
                break;

            case 29: //perform syscall
                //System.out.println("SYSCALL");
                if (!kernelMode) {
                    kernelMode = true;
                    write(1999, SP);
                    SP = 1998;
                    write(SP, PC);
                    if (timer == -1) {
                        PC = 1000;
                    } else {
                        PC = 1500;
                    }
                }
                break;

            case 30: //return syscall
                //System.out.println("RETURN SYSCALL");
                if (kernelMode) {
                    PC = fetch(SP);
                    ++SP;
                    SP = fetch(SP);

                    kernelMode = false;
                }
                break;

            case 50: //end
            default:
                done = true;
        }
    }

    /**
     * Initializes memory
     *
     * @param file filename of instruction file
     */
    public void initMem(String file) {
        try {
            mem = Runtime.getRuntime().exec("java Memory " + file);
        } catch (IOException ex) {
            Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
        }
        memIn = new Scanner(mem.getInputStream());
        memOut = new PrintWriter(mem.getOutputStream());
    }

    /**
     * Starts fetch-execute cycle
     */
    public void run() {
        while (!done) {
            if (timer >= TIMER_MAX && !kernelMode) { //if timer has execeded limit and not in kernelMode
                timer = -1;
                IR = 29;
                execute();
            }

            if (!kernelMode) {  //only increment timer if not in interupt already
                ++timer;
            }

            fetch();
            debugPrint("PRE");
            execute();
            debugPrint("POST");
        }
        memOut.println("stop"); //tell memory to stop
        memOut.flush();
    }

    /**
     * If in debug mode, status is printed
     *
     * @param s String appended to print
     */
    private void debugPrint(String s) {
        if (debug) {
            System.out.printf("%-6s TIMER:%-5d PC:%-5d IR:%-5d SP:%-5d AC:%-5d X:%-5d Y:%-5d\n", s, timer, PC, IR, SP, AC, X, Y);
        }
    }

    /**
     *
     * @param args instructions filename, timer, debug status
     */
    public static void main(String[] args) {
        CPU cpu;

        if (args.length > 1) { //if has timer_max value, initialize it
            cpu = new CPU(Integer.valueOf(args[1]));
        } else {
            cpu = new CPU();
        }

        if (args.length > 0) { //setup memory with file
            cpu.initMem(args[0]);
        } else {
            System.out.println("Please include program file");
            return;
        }
        if(args.length > 2) { //if debug exists, set it
            try {   
                debug = Boolean.valueOf(args[2]);
            } catch (Exception e) {
            }
        }

        cpu.run();  //start fetch-execute cycle

        try {
            mem.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
        }

        int exitVal = mem.exitValue();
        System.out.println("\nProcess exited: " + exitVal);

    }
}
