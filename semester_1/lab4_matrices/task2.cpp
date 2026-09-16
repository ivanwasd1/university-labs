#include <iostream>
#include <random>

const int MAX_SIZE_matrix = 500;

void readMatrix (int** matrix, int n) {

    std::cout << "enter n*n elements of matrix:\n";

    for (int i = 0; i < n; i++) {
        matrix[i] = new int[n];
        for (int j = 0; j < n; j++)
            if (!(std::cin >> matrix[i][j]))
                throw "ERROR!!! You must enter number";
    }

}

void fillMatrixRandom (int** matrix, int n, std::mt19937& gener) {

    std::cout << "enter right and left border of your random numbers: ";
    int right_border, left_border;
    if (!(std::cin >> left_border) || !(std::cin >> right_border))
        throw "ERROR!!! You must enter number";


    if (left_border > right_border)
        std::swap(left_border, right_border);

    std::uniform_int_distribution<int> dist(left_border, right_border);

    for (int i = 0; i < n; i++) {
        matrix[i] = new int[n];
        for (int j = 0; j < n; j++)
            matrix[i][j] = dist(gener);
    }

    std::cout << "\nelements of an matrixay filled with random numbers from " <<  left_border << " to " << right_border << " :\n";

}

void PrintOfMatrix (int** matrix, int n) {

    std::cout << "elements of array:\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            std::cout.width(5);
            std::cout << matrix[i][j] << ' ';
        }
        std::cout << '\n';
    }
    std::cout << '\n';

}

void ReplaceZeroLinesOfArray(int** matrix, int n) {

    for (int i = 0; i < n; i++) {
        if (matrix[i][0] == 0) {
            std::swap(matrix[0], matrix[i]);
            break;
        }
    }
    return;

}

int SerchMaxElementOfTopRightTriangle (int** matrix, int n) noexcept {

    int mx = matrix[0][1];
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            mx = std::max(mx, matrix[i][j]);

    return mx;
}

int main()
{

    int n = 0;
    int** matrix = nullptr;

    try {

        std::cout << "enter 0 if you want that matrixay consists of random numbers\nelse enter 1 to get matrixay consists of entered numbers\n";
        bool flag;
        if (!(std::cin >> flag))
            throw "ERROR!!! you must enter either 1 or 0";


        std::cout << "enter the size of matrix[n*n] (n from 1 to 500): ";
        if (!(std::cin >> n)) {
            throw "ERROR!!! You must enter number";
        }
        if (n < 1) {
            throw "ERROR!!! There can be no less than 1 single element in an array";
        }
        if (n > MAX_SIZE_matrix) {
            throw "ERROR!!! Matrix cant be [>500][>500]";
        }
        matrix = new int*[n];


        if (flag)
            readMatrix(matrix, n);
        else {
            std::mt19937 gen(23523);
            fillMatrixRandom(matrix, n, gen);
        }




        PrintOfMatrix(matrix, n);

        ReplaceZeroLinesOfArray(matrix, n);

        std::cout << "Replaced!\n\n";
        PrintOfMatrix(matrix, n);

        int max_element_of_TopRight_triangle = SerchMaxElementOfTopRightTriangle(matrix, n);

        std::cout << "max element of TopRight triangle is " << max_element_of_TopRight_triangle << '\n';
    }
    catch (const char* errorMes) {
        std::cout << errorMes << '\n';
    }

    for (int i = 0; i < n; ++i)
        delete[] matrix[i];
    delete[] matrix;

    return 0;
}
