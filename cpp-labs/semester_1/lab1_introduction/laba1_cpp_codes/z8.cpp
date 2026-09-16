#include <iostream>

/*
Дано 10 вещественных чисел (вводятся с клавиатуры).
Определить,сколько из них меньше своих «соседей»,
т.е. предыдущего и последующего чисел.
*/

int main()
{
    using std::cin;
    using std::cout;

    int a, b, c, cnt = 0;
    cout << "enter 10 numbers: ";
    if(!(cin >> a >> b)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    for (int i = 0; i < 8; i++) {
        if(!(cin >> c)) {
        cout << "ERROR!!!";
        std::exit(1);
        }
        if (b < a && b < c)
            cnt++;

        a = b, b = c;
    }

    cout << "count of this numbers is " << cnt;

    return 0;
}

/*
так как в условии сказано, что считать нужно
числа, которые меньше чисел предыдущего и последущего,
то первое и десятое числа я не учитывал.
*/
