#include <iostream>

/*
Написать программу, которая определяет, является ли шестизначное
число «счастливым» (т.е. сумма первых трёх цифр равняется сумме
последних трёх цифр).
*/

int main()
{
    using std::cin;
    using std::cout;

    int n;
    cout << "enter a six-digit number: ";
    if(!(cin >> n)) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    int a1 = n / 100000;
    int a2 = n / 10000 % 10;
    int a3 = n / 1000 % 10;
    int a4 = n / 100 % 10;
    int a5 = n / 10 % 10;
    int a6 = n % 10;

    if (a1 + a2 + a3 == a4 + a5 + a6)
        cout << n << " is happy";
    else
        cout << n << " is not happy";

    return 0;
}
