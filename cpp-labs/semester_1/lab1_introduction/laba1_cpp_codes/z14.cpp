#include <iostream>

/*
Вычислите сумму цифр целого неотрицательного числа.
Число вводится с клавиатуры.
*/

int main()
{
    using std::cin;
    using std::cout;

    int n, s = 0;
    cout << "enter the n: ";
    if(!(cin >> n) || n < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    while (n) {
        s += n % 10;
        n /= 10;
    }
    cout << "sum of digits is " << s;


    return 0;
}
