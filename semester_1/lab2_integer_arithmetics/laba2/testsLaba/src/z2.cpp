#include <iostream>

/*
С клавиатуры вводится натуральное число n. Удалить из десятичной
записи этого числа все цифры, повторяющиеся четное количество раз.
*/

void delete_i_digits (int & num, int digit_to_delete) {

    int temp = num, new_num_reverse = 0;

    while (temp > 0) {
        int digit = temp % 10;
        if (digit != digit_to_delete) {
            new_num_reverse *= 10;
            new_num_reverse += digit;

        }
        temp /= 10;
    }

    int new_num = 0;
    temp = new_num_reverse;

    while (temp > 0) {
        int digit = temp % 10;
        new_num *= 10;
        new_num += digit;
        temp /= 10;
    }
    num = new_num;

    return;
}

int solve(int num)
{
    using std::cin;
    using std::cout;

    if (num < 1) {
        std::exit(1);
    }

    for (int i = 0; i <= 9; i++) {

        int temp = num, cnt_of_i = 0;
        while (temp > 0) {

            int idigit = temp % 10;
            if (idigit == i)
                cnt_of_i++;

            temp /= 10;
        }

        if (cnt_of_i != 0 && cnt_of_i % 2 == 0)
            delete_i_digits(num, i);
    }
    if (num != 0)
        return num;

    return -1;
}

