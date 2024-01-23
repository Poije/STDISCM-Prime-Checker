import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

public class PrimeChecker {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the upper bound: ");
        int LIMIT = scanner.nextInt();

        System.out.print("Enter the number of threads: ");
        int numThreads = scanner.nextInt();

        List<int[]> partitioned_List = split_range(LIMIT, numThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Integer> primes = new ArrayList<>();
        for (int i = 0; i<numThreads; i++){
            int start = partitioned_List.get(i)[0];
            int end = partitioned_List.get(i)[1];
            executor.submit(() -> {
                List<Integer> temp = get_primes(start, end);
                primes.addAll(temp);
            });
        }
        System.out.println("Primes: " + primes);
        executor.shutdown();
        scanner.close();
    }
// Function to check if a number is prime or not
    public static boolean check_prime(int n) {
        if (n < 2){
            return false;
        }
        for(int i = 2; i * i <= n; i++) {
            if(n % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> get_primes(int start, int end) {
        List<Integer> primes = new ArrayList<>();
        for(int i = start; i <= end; i++) {
            if(check_prime(i)) {
                primes.add(i);
            }
        }
        return primes;
    }

// Split the range into numThreads parts
    public static List<int[]> split_range(int LIMIT, int numThreads) {
        List<int[]> ranges = new ArrayList<>();
        int range = LIMIT / numThreads;
        int start = 0;
        int end = range;
        for(int i = 0; i < numThreads; i++) {
            int[] temp = {start, end};
            ranges.add(temp);
            start = end + 1;
            end += range;
        }
        
        return ranges;
    }


}
