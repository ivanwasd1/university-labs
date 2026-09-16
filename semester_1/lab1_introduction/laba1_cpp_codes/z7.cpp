#include <iostream>

/*
Написать программу, которая вычисляет среднее
арифметическое последовательности дробных чисел,
вводимых с клавиатуры. После ввода пользователем
последнего числа программа выводит среднее арифметическое,
минимальное и максимальное числа в последовательности.
количество чисел в последовательности вводится с клавиатуры.
*/

int main()
{
    using std::cin;
    using std::cout;

    int n;
    cout << "enter the n: ";
    if(!(cin >> n) || n < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    cout << "enter numbers: ";
    double t;
    if(!(cin >> t)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    double mn = t, mx = t, sum = t;

    for (int i = 1; i < n; i++) {

        if(!(cin >> t)) {
            cout << "ERROR!!!";
            std::exit(1);
        }

        sum += t;
        if (t > mx)
            mx = t;
        if (t < mn)
            mn = t;
    }
    double av = sum / n;
    cout << "average number is " << av << '\n';
    cout << "min number is " << mn << '\n';
    cout << "max number is " << mx << '\n';

    return 0;
}
