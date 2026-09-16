#include <iostream>

//Вычислить так называемый «двойной факториал»:

int main()
{
    using std::cin;
    using std::cout;

    int k, prod = 1;
    cout << "enter the k: ";
    if(!(cin >> k)) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    else if (k == 0) {
        cout << "1";
        std::exit(1);
    }
    else if (k < 0) {
        cout << "It is impossible to calculate the factorial of a negative number";
        std::exit(1);
    }
    for (int i = !(k % 2) + 1; i <= k; i += 2)
        prod *= i;

    cout << "k!! = " << prod;

    return 0;
}

