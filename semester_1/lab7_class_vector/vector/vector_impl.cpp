#include "vector_impl.h"

void Vector::isOutRange(size_t idx) const {
    if (idx >= arrSize)
        throw std::out_of_range("You can't emae take idx which more big than your size");
}

void Vector::Reallocate(size_t newCapacity) {
    int* newArr = new int[newCapacity];
    capacity = newCapacity;
    if (arrSize > 0) {
        std::copy(arr, arr + arrSize, newArr);
    }
    delete[] arr;
    arr = newArr;
}

Vector::Vector() noexcept : arr(nullptr), capacity(0), arrSize(0) {}

Vector::Vector(size_t n) : arr(nullptr), capacity(n), arrSize(n) {
    if (n) {
        arr = new int[n];
        for (size_t i = 0; i < n; i++)
            arr[i] = 0;
    }
}

Vector::Vector(std::initializer_list<int> listik) 
    : arr(nullptr), capacity(listik.size()), arrSize(listik.size()) {
    if (capacity != 0) {
        arr = new int[capacity];
        size_t idx = 0;
        for (int number : listik)
            arr[idx++] = number;
    }
}

Vector::Vector (const Vector& other) 
    : arr(nullptr), capacity(other.capacity), arrSize(other.arrSize) {
    if (capacity != 0) {
        arr = new int[capacity];
        if (arrSize > 0) {
            std::copy(other.arr, other.arr + other.arrSize, arr);
        }
    }
}

Vector& Vector::operator=(Vector other) {
    Swap(other);
    return *this;   
}

Vector::~Vector(){
    delete[] arr;
    arr = nullptr;
    capacity = 0;
    arrSize = 0;
}

void Vector::Swap(Vector& other) noexcept {
    std::swap(other.arr, arr);
    std::swap(other.capacity, capacity);
    std::swap(other.arrSize, arrSize);
}

const int& Vector::operator[](size_t idx) const noexcept {
    return arr[idx];
}

int& Vector::operator[](size_t idx) noexcept {
    return arr[idx];
}

const int& Vector::At(size_t idx) const {
    isOutRange(idx);
    return arr[idx];
}

int& Vector::At(size_t idx) {
    isOutRange(idx);
    return arr[idx];
}

size_t Vector::Size() const noexcept {
    return arrSize;
}

size_t Vector::Capacity() const noexcept {
    return capacity;
}

void Vector::PushBack(int newNum) {

    if (capacity <= arrSize) {
        size_t newCapacity = 2 * capacity + (capacity == 0);
        Reallocate(newCapacity);
    }
    arr[arrSize] = newNum;
    arrSize++;
}

void Vector::PopBack() noexcept {
    if (arrSize > 0)
        --arrSize;
}

void Vector::Clear () noexcept {
    arrSize = 0;
}

void Vector::Reserve(size_t newCapacity) {
    if (newCapacity > capacity) {
        Reallocate(newCapacity);
    }
}

std::ostream& operator<<(std::ostream& out, const Vector& vector) {
    out << "[";
    if (vector.Size() != 0) {
        out << vector[0];
        for (size_t i = 1; i < vector.Size(); ++i) {
            out << ", " << vector[i];
        }
    }
    out << "]";
    return out;
}