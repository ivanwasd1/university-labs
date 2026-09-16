#include <iostream>

/*
С клавиатуры вводится четырёхзначное
натуральное число. Выяснить,
является ли оно палиндромом
(т.е. читается одинаково как слева
направо, так и справа налево).
*/

int main()
{
    using std::cin;
    using std::cout;

    int n;
    cout << "enter a four-digit number: ";
    if(!(cin >> n)) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    int a1 = n / 1000;
    int a2 = (n / 100) % 10;
    int a3 = (n / 10) % 10;
    int a4 = n % 10;

    if (a1 == a4 && a2 == a3)
        cout << "it is pallindrom";
    else
        cout << "it is not pallindrom";

    return 0;
}
