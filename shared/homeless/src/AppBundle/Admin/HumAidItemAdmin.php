<?php

namespace AppBundle\Admin;

use AppBundle\Entity\HumAidItem;
use Sonata\AdminBundle\Datagrid\ListMapper;
use Sonata\AdminBundle\Form\FormMapper;

class HumAidItemAdmin extends BaseAdmin
{
    protected $datagridValues = array(
        '_sort_order' => 'ASC',
        '_sort_by' => 'sort',
    );

    protected $translationDomain = 'AppBundle';

    /**
     * @param FormMapper $formMapper
     */
    protected function configureFormFields(FormMapper $formMapper)
    {
        $formMapper
            ->add('name', null, [
                'label' => 'Название',
                'required' => true,
            ])
            ->add('category', 'choice', [
                'label' => 'Категория',
                'choices' => [
                    HumAidItem::CATEGORY_CLOTHES => 'Одежда',
                    HumAidItem::CATEGORY_HYGIENE => 'Гигиена',
                    HumAidItem::CATEGORY_OTHER => 'Другое',
                ],
            ])
            ->add('limitDays', 'number', [
                'label' => 'Лимит дней',
            ]);
    }

    /**
     * @param ListMapper $listMapper
     */
    protected function configureListFields(ListMapper $listMapper)
    {
        $listMapper
            ->addIdentifier('name', null, [
                'label' => 'Название',
            ])
            ->add('category', 'choice', [
                'label' => 'Категория',
                'choices' => [
                    HumAidItem::CATEGORY_CLOTHES => 'Одежда',
                    HumAidItem::CATEGORY_HYGIENE => 'Гигиена',
                    HumAidItem::CATEGORY_OTHER => 'Другое',
                ],
            ])
            ->add('limitDays', 'number', [
                'label' => 'Лимит дней',
            ])
            ->add('_action', null, [
                'label' => 'Действие',
                'actions' => [
                    'edit' => [],
                    'delete' => [],
                ]
            ]);
    }
}
