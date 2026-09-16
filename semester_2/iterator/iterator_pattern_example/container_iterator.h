#pragma once


#include "iterator.h"
#include "simple_container.h"


// iterator implementetion for your specific container
// conatainer also can be template
class ContainerIterator: public Iterator<int> {
private:
    const Container* c_ptr;
    size_t index;
public:
    ContainerIterator(const Container*);

    void First() override;

    void Next() override;

    const int& CurrentItem() const override;

    bool IsDone() const override;

};

