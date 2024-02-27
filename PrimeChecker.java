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
    public static List<Integer> get_primes(int start, int end) {
        List<Integer> primes = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            if (check_prime(i)) {
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
        int end = range - 1; // Adjust for inclusive ranges

        for (int i = 0; i < numThreads; i++) {
            if (i == numThreads - 1) {
                // Ensure the last range goes up to LIMIT
                end = LIMIT;
            }
            ranges.add(new int[]{start, end});
            start = end + 1;
            end += range;
        }

        return ranges;
    }
}
