#include <iostream>

/*
С клавиатуры вводится натуральное число n. Добавить слева и справа
от этого числа наименьшую отличную от нуля цифру.
*/

int main()
{
    using std::cin;
    using std::cout;

    int num;
    cout << "Enter the number: ";
    if (!(cin >> num) || num < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }

    int min_digit = 9, temp = num, mult = 1;
    while (temp) {

        int digit = temp % 10;
        if (min_digit > digit && digit != 0) {
            min_digit = digit;
        }

        temp /= 10;
        mult *= 10;
    }

    num += mult * min_digit;
    num *= 10;
    num += min_digit;

    cout << "New number: " << num;

    return 0;
}
