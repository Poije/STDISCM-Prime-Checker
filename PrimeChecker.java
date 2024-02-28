import java.util.ArrayList;
import java.util.List;

public class PrimeChecker {

    // Function to check if a number is prime or not
    public static boolean check_prime(int n) {
        if (n < 2) {
            return false;
        }
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    // Generate prime numbers within a given range
    public static List<Integer> get_primes(int start, int end, int numThreads) {
        List<Integer> primes = new ArrayList<>();
        List<int[]> partitioned_List = split_range(start, end, numThreads);
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++){
            int[] threadRange = partitioned_List.get(i);
            threads[i] = new Thread(() -> {
                List<Integer> threadPrimes = thread_get_primes(threadRange[0], threadRange[1]);
                synchronized (primes) {
                    primes.addAll(threadPrimes);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        return primes;
    }

    public static List<Integer> thread_get_primes(int start, int end){
        List<Integer> primes = new ArrayList<>();

        for (int i = start; i <= end; i++) {
            if (check_prime(i)) {
                primes.add(i);
            }
        }
        return primes;
    }

    // Split the range into numThreads parts
    public static List<int[]> split_range(int start, int end, int numThreads) {
        List<int[]> ranges = new ArrayList<>();
        int range = (end - start + 1) / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int tempStart = start + i * range;
            int tempEnd = i == numThreads - 1 ? end : tempStart + range - 1;
            ranges.add(new int[]{tempStart, tempEnd});
        }

        return ranges;
    }
}
