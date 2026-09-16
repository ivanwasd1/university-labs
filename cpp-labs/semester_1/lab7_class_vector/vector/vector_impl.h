#pragma once
#include <iostream>
#include <algorithm>
#include <cstddef>
#include <initializer_list>
#include <stdexcept>
#include <utility>

class Vector {

    private:
        int* arr;
        size_t capacity;
        size_t arrSize;
        void isOutRange(size_t idx) const;
        void Reallocate(size_t newCapacity);
    public:
        Vector() noexcept;
        explicit Vector(size_t n);
        Vector(std::initializer_list<int> listik);
        Vector(const Vector& other);
        Vector& operator=(Vector other);
        ~Vector();

        void Swap(Vector& other) noexcept;
        
        const int& operator[](size_t idx) const noexcept; 
        int& operator[](size_t idx) noexcept;

        const int& At(size_t idx) const;
        int& At(size_t idx);

        size_t Size() const noexcept;
        size_t Capacity() const noexcept;

        void PushBack(int newNum);
        void PopBack() noexcept;
        void Clear() noexcept;
        void Reserve(size_t newCapacity);
};

std::ostream& operator<<(std::ostream& out, const Vector& vector);
