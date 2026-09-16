#include <iostream>
#include <cmath>

/*
С клавиатуры вводится натуральное число n. Найти все совершенные
числа, не превосходящие n.
*/

bool perfect(int num) {

    int sum = 1;
    for (int i = 2; i <= sqrt(num); i++)
        if (!(num % i))
            sum += i + num / i;\

    if (sum == num)
        return 1;

    return 0;
}

int main()
{
    using std::cin;
    using std::cout;

    int n;
    cout << "enter the n: ";
    if (!(cin >> n) || n < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    cout << "perfect numbers which less then n:\n";
    for (int i = 1; i <= n; i++)
        if (perfect(i))
            cout << i << '\n';

    return 0;
}
