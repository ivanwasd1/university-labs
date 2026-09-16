#include <iostream>

/*
С клавиатуры вводится натуральное число n. Удалить из десятичной
записи этого числа все цифры, совпадающие с минимальной цифрой.
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

    int temp = num, min_digit = num % 10;

    while (temp > 0) {
        min_digit = std::min(min_digit, temp % 10);
        temp /= 10;
    }

    temp = num;
    int new_num_reverse = 0;

    while (temp > 0) {
        if (min_digit != temp % 10) {
            new_num_reverse *= 10;
            new_num_reverse += temp % 10;
        }
        temp /= 10;
    }

    int new_num = 0;

    while (new_num_reverse > 0) {
        new_num *= 10;
        new_num += new_num_reverse % 10;
        new_num_reverse /= 10;
    }

    cout << "New number: " << new_num;


    return 0;
}
