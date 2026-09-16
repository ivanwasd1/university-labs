#include <iostream>

/*
Вычислить первые n чисел Фибоначчи.
Число n вводится с клавиатуры.
Числа Фибоначчи вычисляются по формуле:
Fn = Fn−1 + Fn−2, где F0 = 0, F1 = 1.
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
    int a = 0, b = 1;
    cout << a << ' ' << b << ' ';
    for (int i = 0; i < n - 2; i++) {

        cout << a + b << ' ';

        b = a + b;
        a = b - a;
    }

    return 0;
}
