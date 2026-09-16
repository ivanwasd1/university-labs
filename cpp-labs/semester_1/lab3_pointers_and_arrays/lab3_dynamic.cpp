#include <iostream>
#include <random>

const int MAX_SIZE_ARR = 100000;

void read_arr(double* arr, int sizeArr) {
    std::cout << "enter n elements of array (elements must be from 1 to 100000):\n";

    for (int i = 0; i < sizeArr; i++)
        if (!(std::cin >> arr[i])) {
            std::cout << "ERROR!!!";
            delete[] arr;
            std::exit(1);
        }
}

void fill_rand(double* arr, int sizeArr, std::mt19937& gen) {
    std::cout << "enter right and left border of your random numbers: ";
    double right_border, left_border;
    if (!(std::cin >> left_border) || !(std::cin >> right_border)) {
        std::cout << "ERROR!!!";
        delete[] arr;
        std::exit(1);
    }

    if (left_border > right_border)
        std::swap(left_border, right_border);

    std::uniform_real_distribution<double> dist(left_border, right_border);

    for (int i = 0; i < sizeArr; i++)
        arr[i] = dist(gen);

    std::cout << "elements of an array filled with random numbers from " << left_border << "to " << right_border << " :\n";

    for (int i = 0; i < sizeArr; i++)
        std::cout << arr[i] << ' ';
    std::cout << '\n';
}

void segment_max_sum(double* arr, int sizeArr) {
    int now_left = 0, now_right = 0, best_left = 0, best_right = 0;
    double now_sum = arr[0], best_sum = arr[0];

    for (int i = 1; i < sizeArr; i++) {
        if (now_sum > 0) {
            now_sum += arr[i];
            now_right = i;
        } else {
            now_sum = arr[i];
            now_left = i, now_right = i;
        }
        if (now_sum > best_sum) {
            best_sum = now_sum;
            best_left = now_left;
            best_right = now_right;
        }
    }
    std::cout << "the chain of elements having the largest sum:\n";
    for (int i = best_left; i <= best_right; i++)
        std::cout << arr[i] << ' ';
    std::cout << '\n';
}

double sum_after_last_zero(double* arr, int sizeArr) {
    
    if (arr[sizeArr - 1] == 0) {
        cout << "Last element is zero so we can't solve sum\n";
    }
    
    double sum = 0;
    bool found_zero = false;

    for (int i = sizeArr - 1; i >= 0; i--) {
        if (arr[i] == 0) {
            found_zero = true;
            break;
        }
        sum += arr[i];
    }

    if (!found_zero) {
        std::cout << "there is not element zero\n";
        return 0;
    }

    std::cout << "the sum of the array elements located after the last element is zero:  ";
    std::cout << sum << '\n';
    return sum;
}

void transform_array(double* arr, int sizeArr) {

    for (int i = 0; i < sizeArr; i++) {
        if (int(arr[i]) <= 1) {
            for (int j = i; j > 0 && int(arr[j - 1]) > 1; j--) {
                std::swap(arr[j], arr[j - 1]);
            }
        }
    }

}

void print_array(double* arr, int sizeArr) {
    for (int i = 0; i < sizeArr; i++)
        std::cout << arr[i] << ' ';
    std::cout << '\n';
}

int main() {

    using std::cin;
    using std::cout;

    cout << "enter 0 if you want that array consists of random numbers\nelse enter 1 to get array consists of entered numbers\n";
    bool flag;
    if (!(cin >> flag)) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    cout << "enter the size of array (size must be from 1 to 100000) n: ";
    int sizeArr;
    if (!(cin >> sizeArr) || sizeArr < 1 || sizeArr > MAX_SIZE_ARR) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    double* arr = new double[sizeArr];

    if (flag)
        read_arr(arr, sizeArr);
    else {
        std::mt19937 gen(345798);
        fill_rand(arr, sizeArr, gen);
    }

    segment_max_sum(arr, sizeArr);
    sum_after_last_zero(arr, sizeArr);
    transform_array(arr, sizeArr);

    cout << "Your new array:\n";
    print_array(arr, sizeArr);

    delete[] arr;

    return 0;
}
