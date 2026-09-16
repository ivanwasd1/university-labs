#include <iostream>

/*
Дана арифметическая прогрессия: ak = ak−1 + d. Вывести все члены
этой прогрессии, кратные 3, которые лежат на отрезке [a, b]. Числа a, b
и d вводятся с клавиатуры(a0 = a).
*/

int main()
{
    using std::cin;
    using std::cout;

    int a, b, d;
    cout << "enter 3 numbers (the boundary and the difference of the progression): ";

    if (!(cin >> a >> b >> d)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    if (d == 0) {
        cout << "d can't be zero";
        std::exit(1);
    }

    if (a > b && d > 0)
        std::swap(a, b);
    if (a < b && d < 0)
        std::swap(a, b);

    if (a < b) {
        for (int i = a; i <= b; i += d)
            if (!(i % 3))
                cout << i << ' ';
    }
    else {
        for (int i = a; i >= b; i += d)
            if (!(i % 3))
                cout << i << ' ';
    }

    return 1;
}
