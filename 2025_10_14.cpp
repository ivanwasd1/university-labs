#include <iostream>

using namespace std;

void Print_Arr (int* arr, int size_of_arr) {

    for (int i = 0; i < size_of_arr; i++)
        std::cout << *(arr++) << ' ';

}

void Read_Arr (int* arr, int size_of_arr) {

    for (int i = 0; i < size_of_arr; i++)
        std::cin >> *(arr++);

}


void Sort_Of_Arr (int* arr, int size_of_arr) {

    int* starti = arr;
    int* endi = arr + size_of_arr;

    for (int* i = starti; i < endi - 1; i++)
        for (int* j = i + 1; j < endi; j++)
            if (std::abs(*i) > std::abs(*j))
                std::swap(*i, *j);

}

int main()
{
    using std::cin;
    using std::cout;


    int size_of_arr;
    if (!(cin >> size_of_arr)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    int* arr = new int [size_of_arr];
    Read_Arr(arr, size_of_arr);

    Sort_Of_Arr(arr, size_of_arr);
    Print_Arr(arr, size_of_arr);
}
