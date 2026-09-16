#include <iostream>
#include <cmath>

/*
С клавиатуры вводится натуральное число n.
Найти все двойные палиндромы, не превосходящие n.
*/

bool Ispol(int num) {

    bool result = 1;

    int num_reverse = 0, cnt_of_digits = 0, temp = num;

    while (temp) {
        num_reverse *= 10;
        num_reverse += temp % 10;
        temp /= 10;
        cnt_of_digits++;
    }

    for (int i = 0; i < cnt_of_digits / 2; i++) {

        int digit = num % 10;
        int digit_reverse = num_reverse % 10;

        if (digit != digit_reverse)
            result = 0;

        num /= 10;
        num_reverse /= 10;
    }


    return result;
}

int main()
{
    using std::cin;
    using std::cout;

    int num;
    if (!(cin >> num) || num < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    if (Ispol(num) && Ispol(num*num))
        cout << "YES";
    else
        cout << "NO";

    return 0;
}
