#include <iostream>
#include <random>

const int maxSize = 500;

void bubbleSort (int* arr, int arrSize) {

    for (int i = 0; i < arrSize; i++)
        for (int j = 0; j < arrSize - i - 1; j++)
            if (arr[j] > arr[j + 1])
                std::swap(arr[j], arr[j + 1]);

}

void bubbleSortMatrix (int** arr, int arrSize) {

    for (int i = 0; i < arrSize; i++)
        bubbleSort(arr[i], arrSize);

}

void insertionSort (int* arr, int arrSize) {

    for (int i = 1; i < arrSize; i++)
        for (int j = i - 1; j >= 0 && arr[j] > arr[j + 1]; j--)
            std::swap(arr[j], arr[j + 1]);

}

void insertionSortMatrix (int** arr, int arrSize) {

    for (int i = 0; i < arrSize; i++)
        insertionSort(arr[i], arrSize);

}

void choiceSort (int* arr, int arrSize) {

    int minIdx;
    for (int i = 0; i < arrSize - 1; i++) {
        minIdx = i;
        for (int j = i + 1; j < arrSize; j++)
            if (arr[j] < arr[minIdx])
                minIdx = j;
        std::swap(arr[i], arr[minIdx]);
    }

}

void choiceSortMatrix (int** arr, int arrSize) {

    for (int i = 0; i < arrSize; i++)
        choiceSort(arr[i], arrSize);

}

void mergeArr (int *arr, int left, int mid, int right, int *temp) {

    int iLeft = left, iMid = mid, It = left;
    while (iLeft < mid && iMid < right)
        if (arr[iLeft] < arr[iMid])
            temp[It++] = arr[iLeft++];
        else
            temp[It++] = arr[iMid++];

    while (iLeft < mid)
        temp[It++] = arr[iLeft++];
    while (iMid < right)
        temp[It++] = arr[iMid++];

    for (int i = left; i < right; i++)
        arr[i] = temp[i];

}

void mergeSort (int *arr, int left, int right, int *temp) {

    if (left + 1 >= right)
        return;

    int mid = (left + right) / 2;
    mergeSort(arr, left, mid, temp);
    mergeSort(arr, mid, right, temp);
    mergeArr(arr, left, mid, right, temp);

}

void mergeSortMatrix (int** arr, int arrSize) {

    int *temp = new int[arrSize];
    for (int i = 0; i < arrSize; i++)
        mergeSort(arr[i], 0, arrSize, temp);

     delete[] temp;

}

void countingSort(int* arr, int arrSize) {

    int minVal = arr[0], maxVal = arr[0];
    for (int i = 1; i < arrSize; i++) {
        if (arr[i] < minVal) minVal = arr[i];
        if (arr[i] > maxVal) maxVal = arr[i];
    }


    int range = maxVal - minVal + 1;
    int* cnt = new int[range]();


    for (int i = 0; i < arrSize; i++)
        cnt[arr[i] - minVal]++;


    int Iter = 0;
    for (int i = 0; i < range; i++)
        while (cnt[i]--)
            arr[Iter++] = i + minVal;

    delete[] cnt;
}

void countingSortMatrix (int** arr, int arrSize) {

    for (int i = 0; i < arrSize; i++)
        countingSort(arr[i], arrSize);

}

void ChooseSort (int** arr, int arrSize) {

    std::cout << "enter the 1 to Bubble sort\n";
    std::cout << "enter the 2 to Insertion sort\n";
    std::cout << "enter the 3 to choiceSort\n";
    std::cout << "enter the 4 to Merge sort\n";
    std::cout << "enter the 5 to Counting sort\n";

    int choice;
    if (!(std::cin >> choice)) {
        std::cout << "ERROR!!!";
        std::exit(1);
    }

    switch (choice)
    {
    case 1:
        bubbleSortMatrix(arr, arrSize);
        break;
    case 2:
        insertionSortMatrix(arr, arrSize);
        break;
    case 3:
        choiceSortMatrix(arr, arrSize);
        break;
    case 4:
        mergeSortMatrix(arr, arrSize);
        break;
    case 5:
        countingSortMatrix(arr, arrSize);
        break;
    default:
        std::cout << "ERROR!!!";
        std::exit(1);
    }

}

void fillArr (int** arr, int arrSize) {

    std::mt19937 gen(2345);
    std::uniform_int_distribution<int> dist(-10, 10);
    for (int i = 0; i < arrSize; i++) {
        arr[i] = new int[arrSize];
        for (int j = 0; j < arrSize; j++)
            arr[i][j] = dist(gen);
    }

}

void printMatrix (int** arr, int arrSize) {

    std::cout << '\n';
    for (int i = 0; i < arrSize; i++) {
        for (int j = 0; j < arrSize; j++) {
            std::cout.width(5);
            std::cout << arr[i][j] << ' ';
        }
        std::cout << '\n' << '\n';
    }

}

int main()
{
    using std::cin;
    using std::cout;

    cout << "enter n (n - size of matrix n * n): ";
    int arrSize;
    if (!(cin >> arrSize)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    int **arr = new int *[arrSize];

    fillArr(arr, arrSize);
    printMatrix(arr, arrSize);
    ChooseSort(arr, arrSize);
    printMatrix(arr, arrSize);


    for (int i = 0; i < arrSize; i++)
        delete[] arr[i];
    delete[] arr;

    return 0;

}
