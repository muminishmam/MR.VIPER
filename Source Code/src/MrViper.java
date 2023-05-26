/**
 * Mr viper is a data mining algorithm for finding minimal rare itemsets.
 * Acknowledgements and libraries used: spmf, Philippe Fournier-Viger
 * this implementation uses spmf library
 * */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import ca.pfv.spmf.tools.MemoryLogger;
import ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset;
public class MrViper {

    // variables
    protected int k ;   // this variable keeps track of what level we are currently at
    protected int minsup;   // user defined threshold
    Map<Integer, BitSet> mapItemTIDS = new HashMap<Integer, BitSet>();  // variables for counting support of distinct items

    int maxItemsetSize = 0;     // this is the maximum transactional length // using this to calculate the level we need to stop

    long startTimestamp = 0; // start time of latest execution
    long endTimeStamp = 0; // end time of latest execution

   BufferedWriter writer = null;     // object to write the output file

    // the number of rare itemsets found
    private int itemsetCount;
    private int tidcount ;

    // constructor
    public MrViper() {
    }

    // Run algorithm
    public void run(double minsupp, String input,String output) throws IOException {

        // output will be sent to file
        writer = new BufferedWriter(new FileWriter(output));
        startTimestamp = System.currentTimeMillis();
        // Step 1: Read the database and put distinct items in Map
        tidcount = 0;   // initialize variable to count the number of transactions

        mapItemTIDS = new HashMap<Integer, BitSet>();
        // key : item   value: tidset of the item as a bitset
        // record the start time


        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line;
        // for each line (transaction) until the end of file
        while (((line = reader.readLine()) != null)) {
            // check memory usage
            MemoryLogger.getInstance().checkMemory();

            // if the line is  a comment, is  empty or is a
            // kind of metadata
            if (line.isEmpty() == true ||
                    line.charAt(0) == '#' || line.charAt(0) == '%'
                    || line.charAt(0) == '@') {
                continue;
            }

            // split line into items according to spaces
            String[] lineSplited = line.split(" ");

            if(maxItemsetSize<lineSplited.length){
                maxItemsetSize = lineSplited.length;
            }
            // for each item
            for (int i=0;i<lineSplited.length;i++) {
                // convert from string to integer
                int item = Integer.parseInt(lineSplited[i]);
                // update the tidset of the item
                BitSet tids = mapItemTIDS.get(item);
                if (tids == null) {
                    tids = new BitSet();
                    mapItemTIDS.put(item, tids);
                } // end if

                tids.set(tidcount);
            } //end for

            tidcount++;  // increase the transaction count

        } // end while

        reader.close(); // close the input file

        minsup= (int) Math.ceil(minsupp * tidcount); // setting relative minsup
        k =1;

    //  Step 2:    build level 1 itemset and scan the database to get frequencies if an element is not frequent save it to file as it ios an minimal rare itemset.
        List<Itemset> L_k1 =new ArrayList<>();

        for(Entry<Integer,BitSet> entry: mapItemTIDS.entrySet()){

            int support = entry.getValue().cardinality(); // get the support count (cardinality of the tidset)
            Integer item = entry.getKey();
            Itemset itemset = new Itemset(item);
            itemset.setTIDs(mapItemTIDS.get(item),support);
            if(support>=minsup){
                L_k1.add(itemset);
            } else{
                saveItemsetToFile(itemset);
            }

        }
        k++;
        if (L_k1.size()==0){
          endTimeStamp = System.currentTimeMillis();
          if(writer!=null){
              writer.close();
          }
          exit();
          System.out.println("exited at 1");
        return;
        }

    // Step 3: Sort the List of frequents in order of increasing support
        Collections.sort(L_k1, new Comparator<Itemset>() {
            public int compare(Itemset o1, Itemset o2) {
                return o1.cardinality - o2.cardinality;
            }
        });
        Collections.sort(L_k1, new Comparator<Itemset>() {
                    public int compare(Itemset o1, Itemset o2) {
                        return o1.get(0) - o2.get(0);
                    }
                });

        // get frequent2itemsets
        // merge single takes a list of 1-frequents and returns a list of frequent-2s
        L_k1= mergeSingle(L_k1,minsup);

        Collections.sort(L_k1, new Comparator<Itemset>() {
            public int compare(Itemset o1, Itemset o2) {
                return o1.cardinality - o2.cardinality;
            }
        });
        Collections.sort(L_k1, new Comparator<Itemset>() {
                    public int compare(Itemset o1, Itemset o2) {
                        return o1.get(0) - o2.get(0);
                    }
                });
        k++;

        // loop through frequents until no frequents are left
        while(L_k1!=null && L_k1.size()>0){

            MemoryLogger.getInstance().checkMemory();

           // generatecandidate(L_k1) take a list of frequents k-1 and produces a list of frequents of level k
             L_k1 = generatecandidate(L_k1);

            // sort given list in increasing order of support
            if(L_k1!=null){
            Collections.sort(L_k1, new Comparator<Itemset>() {
                public int compare(Itemset o1, Itemset o2) {
                    return o1.cardinality - o2.cardinality;
                }
            });
                Collections.sort(L_k1, new Comparator<Itemset>() {
                    public int compare(Itemset o1, Itemset o2) {
                        return o1.get(0) - o2.get(0);
                    }
                });
            }

            k++;
        }
        // record end time
        // check the memory usage
        endTimeStamp = System.currentTimeMillis();
        MemoryLogger.getInstance().checkMemory();

        writer.close(); // close the file

        // exit
        exit();
    } //run

    // method to to produce L_k2

   public List<Itemset> mergeSingle(List<Itemset> L1, int minsup) throws IOException {
        List<Itemset> result = new ArrayList<Itemset>();
        for(int i = 0; i < L1.size() - 1; i++)
        {
            Integer indexI = L1.get(i).get(0);
            BitSet iID = mapItemTIDS.get(indexI); // Store tID of item i

            for(int j = i + 1; j < L1.size(); j++)
            {
                Integer indexJ = L1.get(j).get(0);
                BitSet jID = mapItemTIDS.get(indexJ); // Store tID of item j
                int[] contain = new int[2];
                // Itemset are in an increasing order
                if(indexI < indexJ)
                {
                    contain[0] = indexI;
                    contain[1] = indexJ;
                }else
                {
                    contain[0] = indexJ;
                    contain[1] = indexI;
                }
                Itemset itemset = new Itemset(contain);
                BitSet temp = (BitSet) iID.clone();
                temp.and(jID); // Store tID of i AND j
                int support = temp.cardinality();
                itemset.setTIDs(temp, support);
                if(support >= minsup)
                {
                    result.add(itemset);
                }
                else
                {
                    saveItemsetToFile(itemset);
                }
            } // inner loop
        } // outer loop
        return result;
    } // end mergeSingle

    private List<Itemset> generatecandidate(List<Itemset> Lk_1)throws IOException {
        List<Itemset> result = new ArrayList<>();
        loop1 : for (int i = 0; i < Lk_1.size()-1; i++) {
            Itemset itemset1 = Lk_1.get(i);
            loop2: for (int j = i + 1; j < Lk_1.size(); j++) {
                Itemset itemset2 = Lk_1.get(j);
                boolean valid= true;
                // we compare items of itemset1 and itemset2.
                for (int l = 0; l < itemset1.getItems().length-1; l++) {
                    if(!itemset1.get(l).equals(itemset2.get(l))){
                        valid = false;
                    }
                }
                if(valid ){
                    int [] newitems = new int[itemset1.getItems().length+1];
                    System.arraycopy(itemset1.getItems(),0,newitems,0,itemset1.getItems().length);
                    newitems[newitems.length-1]= itemset2.getLastItem();
                    // NOW COMBINE ITEMSET 1 AND ITEMSET 2
                    Itemset newitemset = new Itemset(newitems);
                    BitSet temp = (BitSet) itemset1.getTransactionsIds().clone();
                    temp.and(itemset2.getTransactionsIds());
                    newitemset.setTIDs(temp, temp.cardinality());
                    if(checksubsets(newitemset,Lk_1)){
                        if (newitemset.cardinality >= minsup) {
                            result.add(newitemset);

                        }else{
                            saveItemsetToFile(newitemset);
                        }
                    }
                }
            }// loop 2
        } // loop1
        return result;
    }

    // method to check that all subsets of an itemset are frequent in previous level
    private boolean checksubsets(Itemset newitemset, List<Itemset> freq) {
        boolean value = true;
        int itemsetsize = newitemset.getItems().length;
        int checksub = freq.get(0).getItems().length;
        if(itemsetsize>freq.size()){
            return false;
        } else{

            int sub[] = newitemset.getItems();
            int temp[]= new int[checksub];
            for(int posRemoved=0; posRemoved <itemsetsize; posRemoved++){
                // create a subset
                int h =0;
                for (int i = 0; i < temp.length; i++) {
                    if(h==posRemoved){
                        ++h;
                    }
                    temp[i]=sub[h];
                    ++h;
                }
                boolean found= false;
                for (int i = 0; i < freq.size() && !found; i++) {
                    int [] listtemp = freq.get(i).getItems();
                    Arrays.sort(listtemp);
                    Arrays.sort(temp);
                    if(Arrays.equals(temp,listtemp)){
                        found = true;
                    }
                }
                if(found==false){
                    return false;
                }
            }
        }
        return value;
    }

    // helper methods

    // method to print an ArrayList of Itemset
    private void print(List<Itemset> l_k1) {
        Collections.sort(l_k1, new Comparator<Itemset>() {
            public int compare(Itemset o1, Itemset o2) {
                return o1.get(0) - o2.get(0);
            }
        });
        for (int i = 0; i < l_k1.size(); i++) {
            System.out.println(" "+l_k1.get(i).toString() +" supp: "+ l_k1.get(i).cardinality);
        }
    }

    // prints stats
    private void exit() {
        System.out.println(" STATISTICS: ");
        MemoryLogger.getInstance().checkMemory();
        System.out.println("Memory usage: "+ MemoryLogger.getInstance().getMaxMemory() + " mb");
        System.out.println("time in ms: "+ (endTimeStamp-startTimestamp));
        System.out.println("Itemset Count:" + itemsetCount);

    }

    void saveItemsetToFile(Itemset itemset) throws IOException {
        writer.write(itemset.toString() + " #SUP: " + itemset.cardinality);
        writer.newLine();
        itemsetCount++; // increase frequent itemset count
    } // saveItemsetToFile

    void saveToFile(Integer key, int cardinality) throws IOException {
        writer.write(key + " #SUP: " + cardinality);
        writer.newLine();
        itemsetCount++; // increase frequent itemset count
    }

} // class



/* other implementations that were used and tried to compare optimal times.

public List<Itemset> mergeMultiple(List<Itemset> frequent, int minsup) throws IOException {
        List<Itemset> result= null;
        // frequent item in the list must >= number items in the next level, and next level is not over the maximum-itemset to generate next level
        if(frequent.size() >= k && k <= maxItemsetSize)
        {   result = new ArrayList<>(); // Initialize the return list
            // Loop through every item from the list except the last one (so it could be merge with the last one)
            for(int i = 0; i < frequent.size() - 1; i++)
            {
                Itemset itemsetI = frequent.get(i); // Get the item
                Integer frsI = itemsetI.get(0); // First element of the itemset I
                BitSet iID = itemsetI.getTransactionsIds(); // Store tID of item i
                int[] itemsI = itemsetI.getItems();
                // If create level 3 only have to check for the first prefix in the 2-itemset only need the whole list if it greater than 3
                if(k > 3)
                {
                    itemsI = itemsetI.getItems(); // get the list of items in the itemset I
                }
                // Loop through the itemset after the I one (no duplicate item in the itemset)
                for(int j = i + 1; j < frequent.size(); j++)
                {
                    Itemset itemsetJ = frequent.get(j);
                    Integer frsJ = itemsetJ.get(0); // First element of the itemset J
                    BitSet jID = itemsetJ.getTransactionsIds(); // Store tID of item J
                    int[] contain = new int[k];
                    if(frsI == frsJ) // If they have the same prefix continue otherwise start the new one
                    {
                        if(k == 3) // only check the first one if in level 3
                        {
                            contain[0] = frsI; // store first item to the itemlist
                            // Compare next item to put into the itemset
                            if(itemsetI.get(1) < itemsetJ.get(1))
                            {
                                contain[1] = itemsetI.get(1);
                                contain[2] = itemsetJ.get(1);
                            }else
                            {
                                contain[1] = itemsetJ.get(1);
                                contain[2] = itemsetI.get(1);
                            }
                            // Create bitset and counting it support to add to the item
                            BitSet temp = (BitSet) iID.clone();
                            temp.and(jID); // Store tID of i AND j
                            int support = temp.cardinality();
                            Itemset itemset = new Itemset(contain, temp, support);
                            if(support >= minsup)
                            {
                                result.add(itemset);
                            }
                            else
                            {
                                if(checksubsets(itemset,frequent))
                                {
                                    saveItemsetToFile(itemset);
                                }
                            }
                        }else
                        {
                            int[] itemsJ = itemsetJ.getItems();
                            contain[0] = frsJ;
                            boolean same = true; // True if I and J have the same prefix, false otherwise
                            for(int l = 1; l < k - 2; l++)
                            {
                                if(itemsI[l] != itemsJ[l])
                                {
                                    same = false;
                                    break;
                                }else
                                {
                                    contain[l] = itemsI[l];
                                }
                            } // for loop
                            // If same prefix
                            if(same == true)
                            {
                                if(itemsetI.get(itemsetI.size()-1) < itemsetJ.get(itemsetI.size()-1))
                                {
                                    contain[k - 2] = itemsetI.get(itemsetI.size()-1);
                                    contain[k - 1] = itemsetJ.get(itemsetI.size()-1);
                                }else
                                {
                                    contain[k - 2] = itemsetJ.get(itemsetI.size()-1);
                                    contain[k - 1] = itemsetI.get(itemsetI.size()-1);
                                }
                                BitSet temp = (BitSet) iID.clone();
                                temp.and(jID); // Store tID of i AND j
                                int support = temp.cardinality();
                                Itemset itemset = new Itemset(contain, temp, support);
                                if(support >= minsup)
                                {
                                    result.add(itemset);
                                }
                                else
                                {
                                    if(checksubsets(itemset,frequent))
                                    {
                                        saveItemsetToFile(itemset);
                                    }
                                } // end if(same)
                            }
                        } // else
                    } // 1st if
                }
            }
        }else
        {
            return null;
        }
        return result;
    } // end mergeMultiple

    // AND k-1 itemset to create k-itemset
    public List<Itemset> mergeMultiple1(List<Itemset> frequent, int minsup)
    {
        List<Itemset> result= null;
        // frequent item in the list must >= number items in the next level, and next level is not over the maximum-itemset to generate next level
        if(frequent.size() >= k && k <= maxItemsetSize)
        {   result = new ArrayList<>();

            for(int i = 0; i < frequent.size() - 1; i++)
            {
                Itemset itemsetI = frequent.get(i);
                Integer frsI = itemsetI.get(0); // First element of the itemset I
                BitSet iID = itemsetI.getTransactionsIds(); // Store tID of item i
                int[] itemsI = itemsetI.getItems();
                if(k > 3)
                {
                     itemsI = itemsetI.getItems();
                }
                for(int j = i + 1; j < frequent.size(); j++)
                {
                    Itemset itemsetJ = frequent.get(j);
                    Integer frsJ = itemsetJ.get(0); // First element of the itemset J
                    BitSet jID = itemsetJ.getTransactionsIds(); // Store tID of item J
                    int[] contain = new int[k];
                    if(frsI == frsJ)
                    {
                        if(k == 3)
                        {
                            contain[0] = frsI;
                            if(itemsetI.get(1) < itemsetJ.get(1))
                            {
                                contain[1] = itemsetI.get(1);
                                contain[2] = itemsetJ.get(1);
                            }else
                            {
                                contain[1] = itemsetJ.get(1);
                                contain[2] = itemsetI.get(1);
                            }
                            BitSet temp = (BitSet) iID.clone();
                            temp.and(jID); // Store tID of i AND j
                            int support = temp.cardinality();
                            Itemset itemset = new Itemset(contain, temp, support);
                            result.add(itemset);
                        }else
                        {
                            int[] itemsJ = itemsetJ.getItems();
                            contain[0] = frsJ;
                            boolean same = true; // True if I and J have the same prefix, false otherwise
                            for(int l = 1; l < k - 2; l++)
                            {
                                if(itemsI[l] != itemsJ[l])
                                {
                                    same = false;
                                    break;
                                }else
                                {
                                    contain[l] = itemsI[l];
                                }
                            }
                            if(same == true)
                            {
                                if(itemsetI.get(itemsetI.size()-1) < itemsetJ.get(itemsetI.size()-1))
                                {
                                    contain[k - 2] = itemsetI.get(itemsetI.size()-1);
                                    contain[k - 1] = itemsetJ.get(itemsetI.size()-1);
                                }else
                                {
                                    contain[k - 2] = itemsetJ.get(itemsetI.size()-1);
                                    contain[k - 1] = itemsetI.get(itemsetI.size()-1);
                                }
                                BitSet temp = (BitSet) iID.clone();
                                temp.and(jID); // Store tID of i AND j
                                int support = temp.cardinality();
                                Itemset itemset = new Itemset(contain, temp, support);
                            }
                        } // else
                    } // 1st if
                }
            }
        }else
        {
            return null;
        }
        return result;
    } // end mergeMultiple
* */

/*   public List<Itemset> mergeSingle(List<Itemset> L1, int minsup) throws IOException {
        List<Itemset> result = new ArrayList<>();
        for(int i = 0; i < L1.size() - 1; i++)
        {
            Integer item1 = L1.get(i).get(0); // we're dealing with just 1 item in itemset so far
            BitSet b1 = mapItemTIDS.get(item1);
            for(int j = i + 1; j < L1.size(); j++)
            {
                Integer item2 = L1.get(j).get(0); // we're dealing with just 1 item in itemset so far
                BitSet b2 = mapItemTIDS.get(item2);

                BitSet temp = (BitSet) b1.clone();
                Itemset itemset = new Itemset(new int[] {item1, item2});
                temp.and(b2);
                int sup= temp.cardinality();
                itemset.setTIDs(temp,sup);

                if(sup >= minsup)
                {
                    result.add(itemset);
                }
                else
                {
                    saveItemsetToFile(itemset);
                }
            } // inner loop
        } // outer loop
        return result;
    } // end mergeSingle
*/