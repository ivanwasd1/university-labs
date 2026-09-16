#include <iostream>
#include <cmath>

/*
Даны два натуральных числа n и m(вводятся с клавиатуры). Вывести
все их общие делители
*/

int my_gcd(int a, int b) {

    if (!(a % b))
        return b;
    if (!(b % a))
        return a;

    if (a > b)
        return my_gcd(a % b, b);
    else
        return my_gcd(a, b % a);
}

int main()
{
    using std::cin;
    using std::cout;

    int n, m;
    cout << "enter n and m: ";
    if(!(cin >> n >> m)) {
        cout << "ERROR!!!";
        std::exit(1);
    } else if (n < 1 || m < 1) {
        cout << "n or(and) m are not natural number";
        std::exit(1);
    }

    int gcd = my_gcd(n, m);

    cout << "common divisors of numbers " << n << " and " << m << ":\n";
    for (int i = 1; i <= gcd; i++)
        if (!(gcd % i))
            cout << i << ' ';


    return 0;
}
