#include "container_iterator.h"


ContainerIterator::ContainerIterator(const Container* ptr)
    : c_ptr(ptr), index(-1) {}
    
void ContainerIterator::First() {
    index = 0;
}

void ContainerIterator::Next() {
    ++index;
}

const int& ContainerIterator::CurrentItem() const {
    // we can avoid marking
    // ContainerIterator as friend in our container
    // using Container::operator[]:
    // (so, we don't need access to field "c_ptr" here):
    // return c_ptr->operator[](index);
    
    return c_ptr->arr_[index];
}

bool ContainerIterator::IsDone() const {
    return index == c_ptr->Size();
}
